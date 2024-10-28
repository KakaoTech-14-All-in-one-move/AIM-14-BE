package com.example.pitching.call.operation;

import com.example.pitching.call.operation.code.Operation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Event(
        @JsonProperty("op") Operation operation,
        Data data,
        @JsonProperty("seq") String sequence
) {
    public static Event of(Operation operation, Data data, String sequence) {
        return new Event(operation, data, sequence);
    }
}
