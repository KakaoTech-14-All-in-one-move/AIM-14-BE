package com.example.pitching.auth.config;

import com.example.pitching.auth.service.JwtAuthenticationEntryPoint;
import com.example.pitching.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class WebfluxSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**", "/oauth2/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return authentication -> {
            if (authentication.getCredentials() != null) {
                String username = authentication.getName();
                String password = authentication.getCredentials().toString();

                return userDetailsService().findByUsername(username)
                        .filter(userDetails ->
                                passwordEncoder().matches(password, userDetails.getPassword()))
                        .map(userDetails -> new UsernamePasswordAuthenticationToken(
                                userDetails.getUsername(),
                                null,
                                userDetails.getAuthorities()
                        ));
            }
            return Mono.empty();
        };
    }

    // 테스트용 임시 사용자 설정
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("user@test.com")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build();
        return new MapReactiveUserDetailsService(user);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // HTTPS 프로토콜 명시
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private AuthenticationWebFilter jwtAuthenticationFilter() {
        ReactiveAuthenticationManager authenticationManager = new ReactiveAuthenticationManager() {
            @Override
            public Mono<Authentication> authenticate(Authentication authentication) {
                String token = authentication.getCredentials().toString();
                try {
                    String username = jwtTokenProvider.validateAndGetUsername(token);
                    return Mono.just(new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.emptyList()
                    ));
                } catch (Exception e) {
                    return Mono.error(e);
                }
            }
        };

        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);

        filter.setServerAuthenticationConverter(exchange -> {
            String token = exchange.getRequest().getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                return Mono.just(new UsernamePasswordAuthenticationToken(token, token));
            }

            return Mono.empty();
        });

        return filter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}