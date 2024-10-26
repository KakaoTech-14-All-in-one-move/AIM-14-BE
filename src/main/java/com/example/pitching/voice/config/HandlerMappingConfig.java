package com.example.pitching.voice.config;

import com.example.pitching.voice.dto.properties.ServerProperties;
import com.example.pitching.voice.handler.ResumeWebSocketHandler;
import com.example.pitching.voice.handler.VoiceWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
public class HandlerMappingConfig {

    @Bean
    public SimpleUrlHandlerMapping simpleUrlHandlerMapping(
            VoiceWebSocketHandler voiceWebSocketHandler, ResumeWebSocketHandler resumeWebSocketHandler, ServerProperties serverProperties) {
        return new SimpleUrlHandlerMapping(Map.of(
                serverProperties.voice().path(), voiceWebSocketHandler,
                serverProperties.voice().resumePath(), resumeWebSocketHandler
        ), 1);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
