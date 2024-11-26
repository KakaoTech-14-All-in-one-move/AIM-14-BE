package com.example.pitching.user.repository;

import com.example.pitching.user.domain.Channel;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChannelRepository extends ReactiveCrudRepository<Channel, Long> {
    Flux<Channel> findByServerId(Long serverId);
    Flux<Channel> findByServerIdOrderByChannelPosition(Long serverId);

    // 서버의 최대 position 값을 가져오는 쿼리 추가
    @Query("SELECT COALESCE(MAX(channel_position), 0) FROM channels WHERE server_id = :serverId")
    Mono<Integer> findMaxPositionByServerId(Long serverId);
}