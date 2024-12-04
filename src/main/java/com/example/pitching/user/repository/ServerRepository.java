package com.example.pitching.user.repository;

import com.example.pitching.user.domain.Server;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ServerRepository extends ReactiveCrudRepository<Server, Long> {
    @Query("""
        SELECT s.* FROM servers s 
        INNER JOIN user_server_memberships usm ON s.server_id = usm.server_id 
        WHERE usm.email = :email
        """)
    Flux<Server> findServersByUserEmail(String email);
}