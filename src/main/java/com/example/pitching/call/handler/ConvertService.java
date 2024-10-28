package com.example.pitching.call.handler;

import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.code.RequestOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConvertService {
    private final ObjectMapper objectMapper;

    public String convertEventToJson(Event event) {
        try {
            return objectMapper.writeValueAsString(event);
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

    public <T extends Data> T readDataFromMessage(String jsonMessage, Class<T> dataClass) {
        try {
            return objectMapper.readValue(objectMapper.readTree(jsonMessage).get("data").toString(), dataClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Event> T convertJsonToEvent(String jsonMessage, Class<T> eventClass) {
        try {
            return objectMapper.readValue(jsonMessage, eventClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + eventClass.getSimpleName(), e);
        }
    }
}
