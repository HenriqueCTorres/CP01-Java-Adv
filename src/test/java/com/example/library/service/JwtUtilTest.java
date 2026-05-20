package com.example.library.service;

import com.example.library.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-for-testing-purposes-only-must-be-at-least-32-chars");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        jwtUtil.init();

        userDetails = new User("testuser", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Should generate a valid JWT token")
    void shouldGenerateToken() {
        String token = jwtUtil.generateToken(userDetails);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        String token = jwtUtil.generateToken(userDetails);
        String extracted = jwtUtil.extractUsername(token);
        assertThat(extracted).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should validate a valid token")
    void shouldValidateValidToken() {
        String token = jwtUtil.generateToken(userDetails);
        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("Should reject token for different user")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtUtil.generateToken(userDetails);
        UserDetails anotherUser = new User("other", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertThat(jwtUtil.isTokenValid(token, anotherUser)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1L); // already expired
        jwtUtil.init();
        String token = jwtUtil.generateToken(userDetails);
        assertThat(jwtUtil.isTokenValid(token, userDetails)).isFalse();
    }
}
