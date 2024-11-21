package com.example.pitching.user.repository;

import com.example.pitching.user.domain.UserServerMembership;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserServerMembershipRepository extends ReactiveCrudRepository<UserServerMembership, String> {
    @Query("DELETE FROM user_server_memberships WHERE server_id = :serverId AND email = :email")
    Mono<Void> deleteByServerIdAndEmail(Long serverId, String email);

    @Query("SELECT * FROM user_server_memberships WHERE server_id = :serverId AND email = :email")
    Mono<UserServerMembership> findByServerIdAndEmail(Long serverId, String email);

    @Query("SELECT COUNT(*) FROM user_server_memberships WHERE server_id = :serverId")
    Mono<Long> countByServerId(Long serverId);
}
