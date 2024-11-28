package com.example.pitching.auth.userdetails;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @InjectMocks
        private CustomUserDetailsService userDetailsService;

        private final String TEST_EMAIL = "test@example.com";
        private final String TEST_USERNAME = "testUser";
        private final String TEST_PASSWORD = "password";
        private User user;

        @BeforeEach
        void setUp() {
                user = User.createNewUser(TEST_EMAIL, TEST_USERNAME, null, TEST_PASSWORD);
                when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Mono.just(user));
        }

        @Test
        @DisplayName("이메일로 UserDetails를 정상적으로 조회할 수 있다")
        void findByEmail() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();

                // then
                assertThat(userDetails).isNotNull();
        }

        @Test
        @DisplayName("UserDetails의 username이 이메일과 일치한다")
        void userDetailsUsernameMatchesEmail() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();

                // then
                assertThat(userDetails.getUsername()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("UserDetails의 권한이 ROLE_USER이다")
        void userDetailsHasRoleUserAuthority() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();

                // then
                assertThat(userDetails.getAuthorities())
                        .extracting("authority")
                        .containsExactly("ROLE_USER");

                verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회시 UsernameNotFoundException을 발생시킨다")
        void findByNonExistentEmail() {
                // given
                String nonExistentEmail = "nonexistent@example.com";
                when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Mono.empty());

                // when & then
                StepVerifier.create(userDetailsService.findByUsername(nonExistentEmail))
                        .expectErrorSatisfies(error -> {
                                assertThat(error)
                                        .isInstanceOf(UsernameNotFoundException.class)
                                        .hasMessageContaining("User not found with email");
                        })
                        .verify();

                verify(userRepository).findByEmail(nonExistentEmail);
        }

        @Test
        @DisplayName("CustomUserDetails가 User 정보를 올바르게 변환한다")
        void customUserDetailsConversion() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();

                // then
                assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        }

        @Test
        @DisplayName("CustomUserDetails의 username이 이메일과 일치한다")
        void customUserDetailsUsernameMatchesEmail() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

                // then
                assertThat(customUserDetails.getUsername()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("CustomUserDetails의 password가 일치한다")
        void customUserDetailsPasswordMatches() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

                // then
                assertThat(customUserDetails.getPassword()).isEqualTo(TEST_PASSWORD);
        }

        @Test
        @DisplayName("CustomUserDetails가 활성화 상태이다")
        void customUserDetailsIsEnabled() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

                // then
                assertThat(customUserDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("CustomUserDetails의 계정이 만료되지 않았다")
        void customUserDetailsAccountNonExpired() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

                // then
                assertThat(customUserDetails.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("CustomUserDetails의 계정이 잠기지 않았다")
        void customUserDetailsAccountNonLocked() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

                // then
                assertThat(customUserDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("CustomUserDetails의 자격 증명이 만료되지 않았다")
        void customUserDetailsCredentialsNonExpired() {
                // when
                UserDetails userDetails = userDetailsService.findByUsername(TEST_EMAIL).block();
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

                // then
                assertThat(customUserDetails.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("리액티브 스트림을 사용한 UserDetails 조회 테스트")
        void reactiveUserDetailsRetrieval() {
                // when & then
                StepVerifier.create(userDetailsService.findByUsername(TEST_EMAIL))
                        .assertNext(userDetails -> {
                                assertThat(userDetails).isNotNull();
                        })
                        .verifyComplete();

                verify(userRepository).findByEmail(TEST_EMAIL);
        }
}