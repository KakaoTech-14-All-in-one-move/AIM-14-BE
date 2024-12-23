package com.example.pitching.user.service;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.user.domain.Server;
import com.example.pitching.user.domain.UserServerMembership;
import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.repository.ServerRepository;
import com.example.pitching.user.repository.UserServerMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ServerServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private ServerRepository serverRepository;
    @Mock
    private UserServerMembershipRepository userServerMembershipRepository;
    @Mock
    private S3FileStorageService fileStorageService;
    @Mock
    private FilePart filePart;
    @Mock
    private ServerMemberCounter memberCounter;

    @InjectMocks
    private ServerService serverService;

    private final String TEST_EMAIL = "test@example.com";
    private final String SERVER_NAME = "Test Server";
    private final String SERVER_IMAGE = "server-image.jpg";
    private final Long SERVER_ID = 1L;

    private Server testServer;
    private User testUser;
    private UserServerMembership testMembership;

    @BeforeEach
    void setUp() {
        testServer = Server.createNewServer(SERVER_NAME, SERVER_IMAGE);
        testUser = User.createNewUser(TEST_EMAIL, "testUser", null, "password");
        testMembership = UserServerMembership.createMembership(TEST_EMAIL, SERVER_ID);

        // 기본 Mock 설정
        lenient().when(serverRepository.save(any(Server.class))).thenReturn(Mono.just(testServer));
        lenient().when(userServerMembershipRepository.save(any(UserServerMembership.class))).thenReturn(Mono.just(testMembership));
        lenient().when(serverRepository.findById(SERVER_ID)).thenReturn(Mono.just(testServer));
        lenient().when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Mono.just(testUser));
        lenient().when(memberCounter.incrementCount(any())).thenReturn(Mono.just(1L));
        lenient().when(memberCounter.decrementCount(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("서버 생성에 성공하면 ServerResponse를 반환한다")
    void createServer_Success() {
        // given
        ServerRequest request = new ServerRequest(SERVER_NAME, SERVER_IMAGE);

        // when & then
        StepVerifier.create(serverService.createServer(request, TEST_EMAIL))
                .assertNext(response ->
                        assertThat(response.server_name()).isEqualTo(SERVER_NAME)
                )
                .verifyComplete();

        verify(serverRepository).save(any(Server.class));
        verify(userServerMembershipRepository).save(any(UserServerMembership.class));
    }

    @Test
    @DisplayName("동시에 여러 유저를 초대할 때 race condition으로 인해 100명 제한이 제대로 동작하지 않는 것을 보여주는 테스트")
    void testConcurrentInviteMemberRaceCondition() {
        // Given
        int numberOfThreads = 150;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        Server testServer = new Server(
                SERVER_ID,
                "Test Server",
                "",
                LocalDateTime.now(),
                new ArrayList<>()
        );

        // ServerMemberCounter mock 설정
        AtomicInteger currentCount = new AtomicInteger(0);
        when(memberCounter.incrementCount(any()))
                .thenAnswer(invocation -> {
                    int count = currentCount.incrementAndGet();
                    if (count > 100) {
                        currentCount.decrementAndGet(); // 롤백
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "서버의 최대 인원(100명)을 초과할 수 없습니다."
                        ));
                    }
                    return Mono.just((long) count);
                });

        when(serverRepository.findById(SERVER_ID)).thenReturn(Mono.just(testServer));
        when(userServerMembershipRepository.findByServerIdAndEmail(any(), any()))
                .thenReturn(Mono.empty());
        when(userRepository.findByEmail(any())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return Mono.just(User.createNewUser(email, "User", null, "password"));
        });
        when(userServerMembershipRepository.save(any(UserServerMembership.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        List<Mono<Void>> invitations = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            String email = "user" + i + "@test.com";
            invitations.add(serverService.inviteMember(SERVER_ID, email));
        }

        StepVerifier.create(
                        Flux.fromIterable(invitations)
                                .parallel(50)
                                .runOn(Schedulers.parallel())
                                .flatMap(mono -> mono
                                        .doOnSuccess(v -> successCount.incrementAndGet())
                                        .onErrorResume(e -> {
                                            if (e instanceof ResponseStatusException &&
                                                    ((ResponseStatusException) e).getStatusCode() == HttpStatus.BAD_REQUEST) {
                                                errorCount.incrementAndGet();
                                            }
                                            return Mono.empty();
                                        }))
                                .sequential()
                )
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        // Then
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(errorCount.get()).isEqualTo(50);
        assertThat(currentCount.get()).isEqualTo(100);

        verify(memberCounter, times(150)).incrementCount(any());
        verify(userServerMembershipRepository, times(100)).save(any());
    }

    @Test
    @DisplayName("멤버 초대에 성공하면 void를 반환한다")
    void inviteMember_Success() {
        // given
        when(userServerMembershipRepository.findByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.empty());
        when(userServerMembershipRepository.save(any(UserServerMembership.class)))
                .thenReturn(Mono.just(testMembership));

        // when & then
        StepVerifier.create(serverService.inviteMember(SERVER_ID, TEST_EMAIL))
                .verifyComplete();

        verify(userServerMembershipRepository).save(any(UserServerMembership.class));
    }

    @Test
    @DisplayName("이미 초대된 멤버를 초대하면 BadRequest 에러를 반환한다")
    void inviteMember_AlreadyExists() {
        // given
        when(userServerMembershipRepository.findByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.just(testMembership));

        // when & then
        StepVerifier.create(serverService.inviteMember(SERVER_ID, TEST_EMAIL))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST
                )
                .verify();
    }

    @Test
    @DisplayName("사용자의 서버 목록을 조회하면 ServerResponse 목록을 반환한다")
    void getUserServers_Success() {
        // given
        when(serverRepository.findServersByUserEmail(TEST_EMAIL))
                .thenReturn(Flux.just(testServer));

        // when & then
        StepVerifier.create(serverService.getUserServers(TEST_EMAIL))
                .assertNext(response ->
                        assertThat(response.server_name()).isEqualTo(SERVER_NAME)
                )
                .verifyComplete();

        verify(serverRepository).findServersByUserEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("서버 이름 업데이트에 성공하면 업데이트된 ServerResponse를 반환한다")
    void updateServerName_Success() {
        // given
        String newName = "New Server Name";
        when(userServerMembershipRepository.findByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.just(testMembership));

        // when & then
        StepVerifier.create(serverService.updateServerName(SERVER_ID, newName, TEST_EMAIL))
                .assertNext(response ->
                        assertThat(response.server_name()).isEqualTo(newName)
                )
                .verifyComplete();

        verify(serverRepository).save(any(Server.class));
    }

    @Test
    @DisplayName("권한이 없는 사용자가 서버 이름 업데이트를 시도하면 Forbidden 에러를 반환한다")
    void updateServerName_Forbidden() {
        // given
        when(userServerMembershipRepository.findByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(serverService.updateServerName(SERVER_ID, "New Name", TEST_EMAIL))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.FORBIDDEN
                )
                .verify();
    }

    @Test
    @DisplayName("서버 이미지 업데이트에 성공하면 새로운 이미지 URL을 반환한다")
    void updateServerImage_Success() {
        // given
        String newImageUrl = "new-server-image.jpg";
        when(userServerMembershipRepository.findByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.just(testMembership));
        when(fileStorageService.store(any(FilePart.class)))
                .thenReturn(Mono.just(newImageUrl));
        when(fileStorageService.delete(any()))
                .thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(serverService.updateServerImage(SERVER_ID, filePart, TEST_EMAIL))
                .expectNext(newImageUrl)
                .verifyComplete();

        verify(fileStorageService).store(any(FilePart.class));
    }

    @Test
    @DisplayName("서버 삭제에 성공하면 void를 반환한다")
    void deleteServer_Success() {
        // given
        when(userServerMembershipRepository.findByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.just(testMembership));
        when(userServerMembershipRepository.deleteByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.empty());
        when(userServerMembershipRepository.countByServerId(SERVER_ID))
                .thenReturn(Mono.just(0L));
        when(serverRepository.deleteById(SERVER_ID))
                .thenReturn(Mono.empty());
        when(fileStorageService.delete(any()))
                .thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(serverService.deleteServer(SERVER_ID, TEST_EMAIL))
                .verifyComplete();

        verify(serverRepository).deleteById(SERVER_ID);
    }

    @Test
    @DisplayName("다른 멤버가 있는 경우 서버 삭제 시 서버는 삭제되지 않는다")
    void deleteServer_WithRemainingMembers() {
        // given
        when(userServerMembershipRepository.findByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.just(testMembership));
        when(userServerMembershipRepository.deleteByServerIdAndEmail(SERVER_ID, TEST_EMAIL))
                .thenReturn(Mono.empty());
        when(userServerMembershipRepository.countByServerId(SERVER_ID))
                .thenReturn(Mono.just(1L));

        // when & then
        StepVerifier.create(serverService.deleteServer(SERVER_ID, TEST_EMAIL))
                .verifyComplete();

        verify(serverRepository, never()).deleteById(SERVER_ID);
    }
}