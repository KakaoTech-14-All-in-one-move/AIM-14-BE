package com.example.pitching.call.handler;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.ReqOp;
import com.example.pitching.call.operation.code.ResOp;
import com.example.pitching.call.operation.res.HeartbeatAck;
import com.example.pitching.call.operation.res.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConvertService {
    private final ObjectMapper objectMapper;

    public String eventToJson(Operation event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ReqOp readReqOpFromMessage(String jsonMessage) {
        try {
            return ReqOp.from(objectMapper.readTree(jsonMessage).get("op").asInt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Response createEventFromJson(String jsonMessage) {
        try {
            ResOp op = ResOp.from(objectMapper.readTree(jsonMessage).get("op").asInt());
            return switch (op) {
                case HEARTBEAT_ACK -> jsonToEvent(jsonMessage, HeartbeatAck.class);
                default -> throw new RuntimeException("Unsupported operation: " + op);
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Operation> T jsonToEvent(String jsonMessage, Class<T> eventClass) {
        try {
            return objectMapper.readValue(jsonMessage, eventClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + eventClass.getSimpleName(), e);
        }
    }
}
