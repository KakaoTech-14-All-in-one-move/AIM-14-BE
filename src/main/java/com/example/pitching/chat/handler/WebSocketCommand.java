package com.example.pitching.chat.handler;

import com.example.pitching.chat.domain.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketCommand {
    private String type; // SUBSCRIBE, SEND, UNSUBSCRIBE
    private String destination; // e.g., /ws/chat/123
    private Long channelId; // Extracted channel ID
    private ChatMessagePayload payload; // Actual message data

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatMessagePayload {
        private Long channelId;
        private String message;
        private String id;        // email을 sender로 사용
        private String type;

        public ChatMessage toChatMessage() {
            return ChatMessage.createTalkMessage(
                    this.channelId,
                    this.id,      // email만 저장
                    this.message
            );
        }
    }
}