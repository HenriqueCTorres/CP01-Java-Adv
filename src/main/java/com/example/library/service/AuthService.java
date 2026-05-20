package com.example.library.service;

import com.example.library.dto.AuthDTO;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.UserRepository;
import com.example.library.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthDTO.TokenResponse register(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username '" + request.getUsername() + "' is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        userRepository.save(user);
        log.info("Registered user: {}", request.getUsername());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return AuthDTO.TokenResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresIn(jwtUtil.getExpiration())
                .build();
    }

    public AuthDTO.TokenResponse login(AuthDTO.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        log.info("User logged in: {}", request.getUsername());

        return AuthDTO.TokenResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresIn(jwtUtil.getExpiration())
                .build();
    }
}
