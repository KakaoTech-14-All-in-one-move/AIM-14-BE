package com.example.pitching.auth.oauth2.handler;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.auth.dto.UserInfo;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements ServerAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final String FRONTEND_REDIRECT_URI = "http://localhost:5173/oauth2/callback";

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        var oAuth2User = oAuth2Token.getPrincipal();

        String email = getEmail(oAuth2Token.getAuthorizedClientRegistrationId(), oAuth2User);
        String name = getName(oAuth2Token.getAuthorizedClientRegistrationId(), oAuth2User);

        return userRepository.findByEmail(email)
                .switchIfEmpty(
                        Mono.defer(() -> {
                            return userRepository.insertUser(email, name, null, "USER")
                                    .then(Mono.just(User.builder()
                                            .email(email)
                                            .username(name)
                                            .role("USER")
                                            .build()));
                        })
                )
                .map(user -> {
                    TokenInfo tokenInfo = jwtTokenProvider.createTokenInfo(email);
                    UserInfo userInfo = new UserInfo(
                            user.getEmail(),
                            user.getUsername(),
                            user.getProfileImage()
                    );
                    return Tuples.of(tokenInfo, userInfo);
                })
                .flatMap(tuple -> {
                    TokenInfo tokenInfo = tuple.getT1();
                    UserInfo userInfo = tuple.getT2();

                    String redirectUrl = UriComponentsBuilder
                            .fromUriString(FRONTEND_REDIRECT_URI)
                            .queryParam("accessToken", tokenInfo.accessToken())
                            .queryParam("refreshToken", tokenInfo.refreshToken())
                            .queryParam("email", userInfo.email())
                            .queryParam("username", userInfo.username())
                            .build()
                            .encode()
                            .toUriString();

                    exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                    exchange.getResponse().getHeaders().setLocation(URI.create(redirectUrl));

                    return exchange.getResponse().setComplete();
                });
    }

    private String getEmail(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

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
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    private String getName(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

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
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }
}