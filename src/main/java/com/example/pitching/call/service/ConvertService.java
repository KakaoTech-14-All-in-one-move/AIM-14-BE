package com.example.pitching.call.service;

import com.example.pitching.call.operation.Data;
import com.example.pitching.call.operation.Event;
import com.example.pitching.call.operation.code.RequestOperation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.example.pitching.call.operation.response.ChannelEnterResponse;
import com.example.pitching.call.operation.response.ChannelLeaveResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ConvertService {
    private final ObjectMapper objectMapper;

    public String convertObjectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<RequestOperation> readRequestOperationFromMessage(String jsonMessage) {
        try {
            RequestOperation requestOperation = RequestOperation.from(objectMapper.readTree(jsonMessage).get("op").asInt());
            return Mono.just(requestOperation);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public Mono<ResponseOperation> readResponseOperationFromMessage(String jsonMessage) {
        try {
            ResponseOperation responseOperation = ResponseOperation.from(objectMapper.readTree(jsonMessage).get("op").asInt());
            return Mono.just(responseOperation);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public <T extends Data> T readDataFromMessage(String jsonMessage, Class<T> dataClass) {
        try {
            return objectMapper.readValue(objectMapper.readTree(jsonMessage).get("data").toString(), dataClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<Event> convertJsonToEventWithSequence(String sequence, String jsonMessage) {
        return readResponseOperationFromMessage(jsonMessage)
                .flatMap(responseOperation -> {
                    if (responseOperation == ResponseOperation.ENTER_CHANNEL_ACK) {
                        return createResponseWithSequence(responseOperation, jsonMessage, ChannelEnterResponse.class, sequence);
                    }
                    if (responseOperation == ResponseOperation.LEAVE_CHANNEL_ACK) {
                        return createResponseWithSequence(responseOperation, jsonMessage, ChannelLeaveResponse.class, sequence);
                    } else {
                        return Mono.error(new RuntimeException("Unsupported response operation: " + responseOperation));
                    }
                });
    }

    private <T extends Data> Mono<Event> createResponseWithSequence(ResponseOperation responseOperation, String jsonMessage, Class<T> dataClass, String sequence) {
        return Mono.just(Event.of(responseOperation, readDataFromMessage(jsonMessage, dataClass), sequence));
    }

    public <T> T convertJsonToData(String jsonMessage, Class<T> dataClass) {
        try {
            return objectMapper.readValue(jsonMessage, dataClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to " + dataClass.getSimpleName(), e);
        }
    }
}
