package com.example.library.service;

import com.example.library.dto.AuthDTO;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.UserRepository;
import com.example.library.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).username("john").password("encoded")
                .role(User.Role.USER).build();

        userDetails = new org.springframework.security.core.userdetails.User(
                "john", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("register - should create user and return token")
    void register_success() {
        AuthDTO.RegisterRequest req = new AuthDTO.RegisterRequest("john", "password");
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtUtil.getExpiration()).thenReturn(3600000L);

        AuthDTO.TokenResponse response = authService.register(req);
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("register - should throw when username taken")
    void register_duplicateUsername() {
        AuthDTO.RegisterRequest req = new AuthDTO.RegisterRequest("john", "pass");
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("taken");
    }

    @Test
    @DisplayName("login - should authenticate and return token")
    void login_success() {
        AuthDTO.LoginRequest req = new AuthDTO.LoginRequest("john", "password");
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtUtil.getExpiration()).thenReturn(3600000L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        AuthDTO.TokenResponse response = authService.login(req);
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("login - should throw on bad credentials")
    void login_badCredentials() {
        AuthDTO.LoginRequest req = new AuthDTO.LoginRequest("john", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
