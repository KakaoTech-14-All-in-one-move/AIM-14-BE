package com.example.pitching.call.service;

import com.example.pitching.call.operation.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UserRegistry {
    private final ConcurrentHashMap<String, UserSession> usersByName = new ConcurrentHashMap<>();
//    private final ConcurrentHashMap<String, UserSession> usersBySessionId = new ConcurrentHashMap<>();

    public void register(UserSession user) {
        usersByName.put(user.getUserId(), user);
//        usersBySessionId.put(user.getSession().getId(), user);
    }

    public UserSession getByName(String name) {
        return usersByName.get(name);
    }

//    public UserSession getBySession(WebSocketSession session) {
//        return usersBySessionId.get(session.getId());
//    }

    public boolean exists(String name) {
        return usersByName.containsKey(name);
    }

//    public UserSession removeBySession(WebSocketSession session) {
//        final UserSession user = getBySession(session);
//        if (user == null) return null;
//        usersByName.remove(user.getUserId());
//        usersBySessionId.remove(session.getId());
//        return user;
//    }

    public UserSession removeByUserId(String userId) {
        final UserSession user = getByName(userId);
        if (user == null) return null;
        usersByName.remove(user.getUserId());
        return user;
    }
}
