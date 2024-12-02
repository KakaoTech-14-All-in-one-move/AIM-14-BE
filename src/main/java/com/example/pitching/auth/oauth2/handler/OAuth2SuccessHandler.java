package com.example.pitching.auth.oauth2.handler;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.auth.dto.UserInfo;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.user.dto.ServerInfo;
import com.example.pitching.user.repository.ChannelRepository;
import com.example.pitching.user.repository.ServerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements ServerAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    @Value("${front.url}")
    private String frontURL;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oAuth2Token.getPrincipal();

        String email = getEmail(oAuth2Token.getAuthorizedClientRegistrationId(), oAuth2User);
        String name = getName(oAuth2Token.getAuthorizedClientRegistrationId(), oAuth2User);

        return userRepository.findByEmail(email)
                .switchIfEmpty(
                        Mono.defer(() -> userRepository.insertUser(email, name, null, "USER")
                                .then(Mono.just(User.createNewUser(email, name, null, null)))
                                .onErrorMap(e -> new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR, "OAuth2 회원가입 처리 중 오류가 발생했습니다."))
                        )
                )
                .flatMap(user -> serverRepository.findServersByUserEmail(user.getEmail())
                        .flatMap(server ->
                                channelRepository.findByServerId(server.getServerId())
                                        .collectList()
                                        .map(channels -> new ServerInfo(
                                                server.getServerId(),
                                                server.getServerName(),
                                                server.getServerImage(),
                                                channels
                                        ))
                        )
                        .collectList()
                        .map(servers -> {
                            TokenInfo tokenInfo = jwtTokenProvider.createTokenInfo(email);
                            UserInfo userInfo = new UserInfo(
                                    user.getEmail(),
                                    user.getUsername(),
                                    user.getProfileImage(),
                                    servers
                            );
                            return Tuples.of(tokenInfo, userInfo);
                        }))
                .flatMap(tuple -> {
                    try {
                        TokenInfo tokenInfo = tuple.getT1();
                        UserInfo userInfo = tuple.getT2();

                        String serversJson = new ObjectMapper()
                                .writeValueAsString(userInfo.servers());

                        String redirectUrl = UriComponentsBuilder
                                .fromUriString(frontURL + "oauth2/callback")
                                .queryParam("accessToken", tokenInfo.accessToken())
                                .queryParam("refreshToken", tokenInfo.refreshToken())
                                .queryParam("email", URLEncoder.encode(userInfo.email(), StandardCharsets.UTF_8))
                                .queryParam("username", URLEncoder.encode(userInfo.username(), StandardCharsets.UTF_8))
                                .queryParam("profile_image", userInfo.profile_image())
                                .queryParam("servers", URLEncoder.encode(serversJson, StandardCharsets.UTF_8))
                                .build()
                                .toUriString();

                        exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                        exchange.getResponse().getHeaders().setLocation(URI.create(redirectUrl));

                        return exchange.getResponse().setComplete();
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "OAuth2 로그인 처리 중 오류가 발생했습니다."));
                    }
                })
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OAuth2 인증 처리 중 오류가 발생했습니다."));
    }

    private String getEmail(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        try {
            return switch (provider) {
                case "google" -> (String) "google@" + attributes.get("email");
                case "kakao" -> {
                    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                    yield (String) "kakao@" + kakaoAccount.get("email");
                }
                case "naver" -> {
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    yield (String) "naver@" + response.get("email");
                }
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 제공자입니다: " + provider);
            };
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "OAuth2 이메일 정보를 가져오는데 실패했습니다.");
        }
    }

    private String getName(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        try {
            return switch (provider) {
                case "google" -> (String) attributes.get("name");
                case "kakao" -> {
                    Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
                    yield (String) properties.get("nickname");
                }
                case "naver" -> {
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    yield (String) response.get("name");
                }
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 제공자입니다: " + provider);
            };
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "OAuth2 사용자 이름을 가져오는데 실패했습니다.");
        }
    }
}