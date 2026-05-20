package com.example.library.config;

import com.example.library.security.JwtAuthFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança para testes @WebMvcTest.
 *
 * O @WebMvcTest não carrega @Configuration do pacote principal automaticamente,
 * então precisamos de uma TestConfiguration que replique as mesmas regras de
 * autorização e registre o JwtAuthFilter no contexto de teste.
 */
@TestConfiguration
@EnableMethodSecurity
@Import(JwtAuthFilter.class)
public class TestSecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/api-docs/**"
    };

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http,
                                               JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/authors/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
