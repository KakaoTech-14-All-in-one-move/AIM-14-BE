package com.example.pitching.user.repository;


import com.example.pitching.user.domain.Channel;
import com.example.pitching.user.domain.utility.ChannelCategory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChannelRepository extends ReactiveCrudRepository<Channel, Long> {
    // 서버 ID로 채널 목록 조회
    Flux<Channel> findByServerId(Long serverId);
}