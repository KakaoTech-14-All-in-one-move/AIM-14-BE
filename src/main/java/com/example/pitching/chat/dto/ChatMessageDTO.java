package com.example.pitching.chat.dto;

import com.example.pitching.auth.domain.User;
import com.example.pitching.chat.domain.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "채팅 메시지 DTO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    @Schema(description = "채널 ID", example = "1")
    private Long channelId;

    @Schema(description = "메시지 타임스탬프 (epoch milliseconds)", example = "1703001234567")
    private Long timestamp;

    @Schema(description = "메시지 고유 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String messageId;

    @Schema(description = "메시지 타입 (ENTER, TALK, LEAVE)", example = "TALK")
    private ChatMessage.MessageType type;

    @Schema(description = "발신자 이메일", example = "user@example.com")
    private String sender;

    @Schema(description = "발신자 현재 이름", example = "홍길동")
    private String senderName;

    @Schema(description = "메시지 내용", example = "안녕하세요!")
    private String message;

    @Schema(description = "발신자 현재 프로필 이미지 URL", example = "https://example.com/images/profile.jpg")
    private String profile_image;

    public static ChatMessageDTO from(ChatMessage message, User user) {
        String finalMessage = message.getMessage();
        if (message.getType() != ChatMessage.MessageType.TALK) {
            finalMessage = user.getUsername() + message.getMessage();
        }

        return new ChatMessageDTO(
                message.getChannelId(),
                message.getTimestamp(),
                message.getMessageId(),
                message.getType(),
                message.getSender(),
                user.getUsername(),
                finalMessage,
                user.getProfileImage()
        );
    }
}