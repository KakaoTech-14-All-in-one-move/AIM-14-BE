package com.example.pitching.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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

    public static Server createNewServer(String serverName, String serverImage) {
        return new Server(null, serverName, serverImage, LocalDateTime.now());
    }
}