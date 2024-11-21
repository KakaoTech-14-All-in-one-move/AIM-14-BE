package com.example.pitching.user.domain;

import com.example.pitching.user.domain.enums.ChannelCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("channels")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Channel {
    @Id
    @Column("channel_id")
    private Long channelId;

    @Column("server_id")
    private Long serverId;

    @Column("channel_name")
    private String channelName;

    @Column("channel_category")
    private ChannelCategory channelCategory;

    @Column("channel_position")
    private Integer channelPosition;

    @Column("created_at")
    private LocalDateTime createdAt;

    public static Channel createNewChannel(Long serverId, String channelName,
                                        ChannelCategory category, Integer position) {
        return new Channel(null, serverId, channelName, category,
                position, LocalDateTime.now());
    }
}
