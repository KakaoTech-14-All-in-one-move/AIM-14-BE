package com.example.pitching.call.operation.request;

import com.example.pitching.call.operation.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OfferRequest(
        @JsonProperty("channel_id")
        Long channelId,
        @JsonProperty("sdp_offer")
        String sdpOffer
) implements Data {
}
