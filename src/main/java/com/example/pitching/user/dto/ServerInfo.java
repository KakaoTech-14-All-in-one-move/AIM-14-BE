package com.example.pitching.user.dto;

import com.example.pitching.user.domain.Channel;
import java.util.List;

public record ServerInfo(
        Long server_id,
        String server_name,
        String server_image,
        List<Channel> channels
) {
}