package com.example.pitching.call.operation;

import com.example.pitching.call.operation.code.Operation;
import com.example.pitching.call.operation.code.ResponseOperation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Events(
        @JsonProperty("op") Operation operation,
        List<Data> data,
        @JsonProperty("seq") String sequence
) {
    public static Events of(Operation operation, List<Data> data, String sequence) {
        return new Events(operation, data, sequence);
    }

    public static Events error(List<Data> data) {
        return Events.of(ResponseOperation.ERROR, data, null);
    }
}
