package com.example.pitching.gateway.config;

import com.example.pitching.gateway.dto.properties.ServerProperties;
import com.example.pitching.gateway.handler.GatewayWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
public class HandlerMappingConfig {

    @Bean
    public SimpleUrlHandlerMapping simpleUrlHandlerMapping(
            GatewayWebSocketHandler gatewayHandler, ServerProperties serverProperties) {
        return new SimpleUrlHandlerMapping(Map.of(
                serverProperties.gateway().path(), gatewayHandler
        ), 1);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
