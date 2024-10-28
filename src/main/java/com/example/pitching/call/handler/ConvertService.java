package com.example.pitching.call.handler;

import com.example.pitching.call.operation.Operation;
import com.example.pitching.call.operation.code.RequestOp;
import com.example.pitching.call.operation.code.ResponseOp;
import com.example.pitching.call.operation.response.HeartbeatAck;
import com.example.pitching.call.operation.response.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConvertService {
    private final ObjectMapper objectMapper;

    public String convertEventToJson(Operation event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public RequestOp readReqOpFromMessage(String jsonMessage) {
        try {
            return RequestOp.from(objectMapper.readTree(jsonMessage).get("op").asInt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Response createResFromJson(String jsonMessage) {
        try {
            ResponseOp responseOp = ResponseOp.from(objectMapper.readTree(jsonMessage).get("op").asInt());
            return switch (responseOp) {
                case HEARTBEAT_ACK -> convertJsonToEvent(jsonMessage, HeartbeatAck.class);
                default -> throw new RuntimeException("Unsupported operation: " + responseOp);
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Operation> T convertJsonToEvent(String jsonMessage, Class<T> eventClass) {
        try {
            return objectMapper.readValue(jsonMessage, eventClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + eventClass.getSimpleName(), e);
        }
    }
}
