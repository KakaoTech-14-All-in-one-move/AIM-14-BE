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
        private String id;        // 프론트엔드에서 보내는 user.id (email)
        private String username;  // 프론트엔드에서 보내는 user.username
        private String profile_image;
        private String type;

        public ChatMessage toChatMessage() {
            return ChatMessage.createTalkMessage(
                    this.channelId,
                    this.id,          // email을 sender로 사용
                    this.username,    // username을 senderName으로 사용
                    this.message,
                    this.profile_image
            );
        }
    }
}