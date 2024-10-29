package com.example.pitching.call.handler;

import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.response.StateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConvertService {
    private final ObjectMapper objectMapper;

    public String convertObjectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public RequestOperation readRequestOperationFromMessage(String jsonMessage) {
        try {
            return RequestOperation.from(objectMapper.readTree(jsonMessage).get("op").asInt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseOperation readResponseOperationFromMessage(String jsonMessage) {
        try {
            return ResponseOperation.from(objectMapper.readTree(jsonMessage).get("op").asInt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Data> T readDataFromMessage(String jsonMessage, Class<T> dataClass) {
        try {
            return objectMapper.readValue(objectMapper.readTree(jsonMessage).get("data").toString(), dataClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Event convertJsonToEventWithSequence(String sequence, String jsonMessage) {
        ResponseOperation responseOperation = readResponseOperationFromMessage(jsonMessage);
        return switch (responseOperation) {
            case ENTER_CHANNEL_ACK ->
                    createResponseWithSequence(responseOperation, jsonMessage, StateResponse.class, sequence);
            default -> null;
        };
    }

    private <T extends Data> Event createResponseWithSequence(ResponseOperation responseOperation, String jsonMessage, Class<T> dataClass, String sequence) {
        return Event.of(responseOperation, readDataFromMessage(jsonMessage, dataClass), sequence);
    }

    public <T> T convertJsonToData(String jsonMessage, Class<T> dataClass) {
        try {
            return objectMapper.readValue(jsonMessage, dataClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + dataClass.getSimpleName(), e);
        }
    }
}
