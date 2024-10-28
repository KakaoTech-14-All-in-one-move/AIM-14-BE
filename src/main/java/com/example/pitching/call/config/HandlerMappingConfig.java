package com.example.pitching.call.config;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.handler.CallWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
public class HandlerMappingConfig {

    @Bean
    public SimpleUrlHandlerMapping simpleUrlHandlerMapping(
            CallWebSocketHandler callWebSocketHandler, ServerProperties serverProperties) {
        return new SimpleUrlHandlerMapping(Map.of(
                serverProperties.getVoicePath(), callWebSocketHandler
        ), 1);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
