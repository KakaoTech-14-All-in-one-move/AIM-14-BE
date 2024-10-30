package com.example.pitching.auth.repository;

import com.example.pitching.auth.domain.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByEmail(String username);
    Mono<Boolean> existsByEmail(String email);
    @Query("INSERT INTO users (email, username, password, role) VALUES (:email, :username, :password, :role)")
    Mono<Void> insertUser(String email, String username, String password, String role);
}