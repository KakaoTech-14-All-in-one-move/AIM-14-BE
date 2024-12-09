package com.example.pitching.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    // Setter for service layer to set generated values
    @Setter
    private String messageId;
    @Setter
    private String timestamp;
    private String channelId;
    private MessageType type;
    private String sender;
    private String content;
    private String profileImage;

    public enum MessageType {
        ENTER, TALK, LEAVE
    }

    public static ChatMessage createChatMessage(String channelId, MessageType type,
                                                String sender, String content, String profileImage) {
        return new ChatMessage(null, null, channelId, type, sender, content, profileImage);
    }

}