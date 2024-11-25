package com.example.pitching.user.domain;

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
    private String channelCategory;

    @Column("channel_position")
    private Integer channelPosition;

    public static Channel createNewChannel(Long serverId, String channelName,
                                        String category, Integer position) {
        return new Channel(null, serverId, channelName, category,
                position);
    }
}
