package com.example.pitching.voice.event.handler;

import com.example.pitching.voice.event.IdentifyEvent;
import com.example.pitching.voice.event.ReadyEvent;
import com.example.pitching.voice.event.op.ReqOp;
import com.example.pitching.voice.event.BeatEvent;
import com.example.pitching.voice.event.GatewayEvent;
import com.example.pitching.voice.event.data.ReadyData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;

@Component
@RequiredArgsConstructor
public class GatewayEventHandler {

    private final ObjectMapper objectMapper;

    public String handleMessage(WebSocketMessage webSocketMessage, String sessionId) {
        ReqOp reqOp = readOpFromMessage(webSocketMessage);
        GatewayEvent event = switch (reqOp) {
            case ReqOp.HEARTBEAT -> BeatEvent.of();
            case ReqOp.IDENTIFY -> identify(webSocketMessage, sessionId);
            default -> throw new RuntimeException("Unknown op code: " + reqOp);
        };
        return eventToJson(event);
    }

    public String eventToJson(GatewayEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends GatewayEvent> T jsonToEvent(String jsonMessage, Class<T> eventClass) {
        try {
            return objectMapper.readValue(jsonMessage, eventClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + eventClass.getSimpleName(), e);
        }
    }

    private GatewayEvent identify(WebSocketMessage webSocketMessage, String sessionId) {
        String token = jsonToEvent(webSocketMessage.getPayloadAsText(), IdentifyEvent.class).getToken();
        // TODO: 인증 처리 후 ReadyData 생성
        ReadyData data = ReadyData.of(null, null, sessionId, null);
        return ReadyEvent.of(data);
    }

    private ReqOp readOpFromMessage(WebSocketMessage webSocketMessage) {
        String jsonMessage = webSocketMessage.getPayloadAsText();
        try {
            return ReqOp.from(objectMapper.readTree(jsonMessage).get("op").asInt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
