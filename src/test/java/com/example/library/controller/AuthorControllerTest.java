package com.example.library.controller;

import com.example.library.config.TestSecurityConfig;
import com.example.library.dto.AuthorDTO;
import com.example.library.entity.Author;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.security.JwtUtil;
import com.example.library.service.AuthorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorController.class)
@Import({TestSecurityConfig.class, AuthorModelAssembler.class})
@ActiveProfiles("test")
@DisplayName("AuthorController Integration Tests (MockMvc)")
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorService authorService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    private Author author;
    private AuthorDTO.Request validRequest;

    @BeforeEach
    void setUp() {
        author = Author.builder()
                .id(1L).name("Machado de Assis")
                .email("machado@example.com")
                .nationality("Brazilian")
                .books(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validRequest = AuthorDTO.Request.builder()
                .name("Machado de Assis")
                .email("machado@example.com")
                .nationality("Brazilian")
                .build();

        // JWT mock: qualquer token é válido e aponta para "admin"
        var adminDetails = new User("admin", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails);
        when(jwtUtil.extractUsername(anyString())).thenReturn("admin");
        when(jwtUtil.isTokenValid(anyString(), any())).thenReturn(true);
    }

    // ---- GET /{id} ----

    @Test
    @DisplayName("GET /api/v1/authors/{id} - 200 when found")
    void findById_found() throws Exception {
        when(authorService.findById(1L)).thenReturn(author);

        mockMvc.perform(get("/api/v1/authors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Machado de Assis"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.books.href").exists());
    }

    @Test
    @DisplayName("GET /api/v1/authors/{id} - 404 when not found")
    void findById_notFound() throws Exception {
        when(authorService.findById(99L)).thenThrow(new ResourceNotFoundException("Author", 99L));

        mockMvc.perform(get("/api/v1/authors/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ---- GET (list / search) — public endpoints ----

    @Test
    @DisplayName("GET /api/v1/authors - 200 without authentication (public)")
    void findAll_public() throws Exception {
        when(authorService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/authors"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/authors/search - 200 without authentication (public)")
    void search() throws Exception {
        Page<Author> page = new PageImpl<>(List.of(author));
        when(authorService.searchByName(eq("Machado"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/authors/search?name=Machado"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/authors/prolific - 200 with HATEOAS links inside _embedded")
    void findProlific() throws Exception {
        when(authorService.findAuthorsWithAtLeastNBooks(1)).thenReturn(List.of(author));

        mockMvc.perform(get("/api/v1/authors/prolific?minBooks=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    // ---- POST ----

    @Test
    @DisplayName("POST /api/v1/authors - 201 when valid + authenticated")
    void create_valid() throws Exception {
        when(authorService.create(any())).thenReturn(author);

        mockMvc.perform(post("/api/v1/authors")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("POST /api/v1/authors - 400 for blank name / invalid email")
    void create_invalid() throws Exception {
        AuthorDTO.Request bad = AuthorDTO.Request.builder()
                .name("").email("not-an-email").build();

        mockMvc.perform(post("/api/v1/authors")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("POST /api/v1/authors - 409 when email already exists")
    void create_duplicateEmail() throws Exception {
        when(authorService.create(any())).thenThrow(new BusinessException("Email already exists"));

        mockMvc.perform(post("/api/v1/authors")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict());
    }

    // ---- PUT ----

    @Test
    @DisplayName("PUT /api/v1/authors/{id} - 200 when valid + authenticated")
    void update_valid() throws Exception {
        when(authorService.update(eq(1L), any())).thenReturn(author);

        mockMvc.perform(put("/api/v1/authors/1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    // ---- DELETE ----

    @Test
    @DisplayName("DELETE /api/v1/authors/{id} - 204 for ADMIN role")
    void delete_asAdmin() throws Exception {
        doNothing().when(authorService).delete(1L);

        // @PreAuthorize("hasRole('ADMIN')") — usamos SecurityMockMvcRequestPostProcessors
        mockMvc.perform(delete("/api/v1/authors/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/authors/{id} - 403 for USER role")
    void delete_asUser_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/authors/1")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
