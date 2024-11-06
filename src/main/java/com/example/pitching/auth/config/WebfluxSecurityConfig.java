package com.example.pitching.auth.config;

import com.example.pitching.auth.exception.InvalidCredentialsException;
import com.example.pitching.auth.exception.InvalidTokenException;
import com.example.pitching.auth.exception.TokenExpiredException;
import com.example.pitching.auth.oauth2.handler.OAuth2FailureHandler;
import com.example.pitching.auth.oauth2.handler.OAuth2SuccessHandler;
import com.example.pitching.auth.jwt.JwtAuthenticationEntryPoint;
import com.example.pitching.auth.jwt.JwtTokenProvider;
import com.example.pitching.auth.userdetails.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class WebfluxSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomUserDetailsService userDetailsService;
    @Value("${front.url}")
    private String frontURL;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**", "/oauth2/**", "/login/oauth2/code/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(oAuth2SuccessHandler)
                        .authenticationFailureHandler(oAuth2FailureHandler)
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // TODO: HTTPS 프로토콜 변경
        configuration.setAllowedOrigins(Arrays.asList(frontURL));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Location")); // Location 헤더 노출

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private AuthenticationWebFilter jwtAuthenticationFilter() {
        ReactiveAuthenticationManager authenticationManager = authentication -> {
            String token = authentication.getCredentials().toString();
            return Mono.just(token)
                    .map(jwtTokenProvider::validateAndGetEmail)
                    .flatMap(userDetailsService::findByUsername)
                    .<Authentication>map(userDetails -> new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    ))
                    .onErrorMap(TokenExpiredException.class,
                            e -> new TokenExpiredException("Token has expired"))
                    .onErrorMap(JwtException.class,
                            e -> new InvalidTokenException("Invalid token"))
                    .onErrorMap(Exception.class,
                            e -> new InvalidCredentialsException("Authentication failed: " + e.getMessage()));
        };

        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);

        filter.setServerAuthenticationConverter(exchange -> {
            String path = exchange.getRequest().getPath().value();
            // OAuth2 관련 경로 추가
            if (path.startsWith("/api/v1/auth/") ||
                    path.startsWith("/oauth2/") ||
                    path.startsWith("/login/oauth2/")) {
                return Mono.empty();
            }

            return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .switchIfEmpty(Mono.error(
                            new AuthenticationCredentialsNotFoundException("Missing Authorization header")
                    ))
                    .filter(authHeader -> authHeader.startsWith("Bearer "))
                    .switchIfEmpty(Mono.error(
                            new InvalidTokenException("Invalid Authorization header format")
                    ))
                    .map(authHeader -> authHeader.substring(7))
                    .map(token -> new UsernamePasswordAuthenticationToken(token, token));
        });

        return filter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}