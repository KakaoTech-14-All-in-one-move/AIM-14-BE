package com.example.pitching.call.service;

import com.example.pitching.call.operation.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.KurentoClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomManager {
    private final KurentoClient kurento;
    private final ConcurrentMap<Long, Room> rooms = new ConcurrentHashMap<>();

    public Room getRoom(Long channelId) {
        Room room = rooms.get(channelId);

        if (room == null) {
            room = Room.of(channelId, kurento.createMediaPipeline());
            log.info("Room {} not existent. Will create now! : {}", channelId, room.getPipeline().getId());
            rooms.put(channelId, room);
        } else {
            log.info("Room {} found! : {}", channelId, room.getPipeline().getId());
        }
        return room;
    }

    public void removeRoom(Room room) {
        this.rooms.remove(room.getChannelId());
        room.close();
        log.info("Room {} removed and closed : {}", room.getChannelId(), room.getPipeline().getId());
    }

    public boolean doesRoomExists(Long channelId) {
        return rooms.containsKey(channelId);
    }
}
