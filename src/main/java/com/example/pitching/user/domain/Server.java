package com.example.pitching.user.domain;

import com.example.pitching.user.domain.enums.ChannelCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Table("servers")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Server {
    @Id
    @Column("server_id")
    private Long serverId;

    @Setter
    @Column("server_name")
    private String serverName;

    @Setter
    @Column("server_image")
    private String serverImage;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient  // DB에 매핑되지 않음을 표시
    private List<Channel> channels = new ArrayList<>();

    public static Server createNewServer(String serverName, String serverImage) {
        return new Server(null, serverName, serverImage, LocalDateTime.now(), new ArrayList<>());
    }

    public List<Channel> createDefaultChannels() {
        if (this.serverId == null) {
            throw new IllegalStateException("Server must be saved before creating channels");
        }

        return Arrays.asList(
                Channel.createNewChannel(this.serverId, "일반", ChannelCategory.CHAT, 1),
                Channel.createNewChannel(this.serverId, "일반", ChannelCategory.VOICE, 2),
                Channel.createNewChannel(this.serverId, "일반", ChannelCategory.VIDEO, 3)
        );
    }
}