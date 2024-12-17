package com.example.pitching.auth.oauth2.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class OAuth2FailureHandler implements ServerAuthenticationFailureHandler {
    @Value("${front.url}")
    private String frontURL;

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange, AuthenticationException exception) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        String redirectUrl = frontURL;
        return Mono.fromRunnable(() -> exchange.getResponse()
                .getHeaders()
                .setLocation(URI.create(redirectUrl)));
    }
}
