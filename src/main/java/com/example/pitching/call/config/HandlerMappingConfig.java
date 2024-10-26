package com.example.pitching.call.config;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.handler.VideoWebSocketHandler;
import com.example.pitching.call.handler.VoiceWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
public class HandlerMappingConfig {

    @Bean
    public SimpleUrlHandlerMapping simpleUrlHandlerMapping(
            VoiceWebSocketHandler voiceWebSocketHandler, VideoWebSocketHandler videoWebSocketHandler, ServerProperties serverProperties) {
        return new SimpleUrlHandlerMapping(Map.of(
                serverProperties.getVoicePath(), voiceWebSocketHandler,
                serverProperties.getVideoPath(), videoWebSocketHandler
        ), 1);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
