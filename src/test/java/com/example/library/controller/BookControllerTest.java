package com.example.library.controller;

import com.example.library.config.TestSecurityConfig;
import com.example.library.dto.AuthorDTO;
import com.example.library.dto.BookDTO;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.security.JwtUtil;
import com.example.library.service.AuthorService;
import com.example.library.service.BookService;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import({TestSecurityConfig.class, BookModelAssembler.class})
@ActiveProfiles("test")
@DisplayName("BookController Integration Tests (MockMvc)")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @MockBean
    private AuthorService authorService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    private Book book;
    private Author author;
    private BookDTO.Request validRequest;

    @BeforeEach
    void setUp() {
        author = Author.builder()
                .id(1L).name("Tolkien").nationality("British")
                .books(new ArrayList<>()).build();

        book = Book.builder()
                .id(1L).title("The Hobbit").isbn("9780547928227")
                .genre("Fantasy").price(new BigDecimal("49.90"))
                .available(true).author(author).build();

        validRequest = BookDTO.Request.builder()
                .title("The Hobbit").isbn("9780547928227")
                .genre("Fantasy").price(new BigDecimal("49.90"))
                .available(true).authorId(1L).build();

        // JWT mock
        var userDetails = new User("user", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        when(jwtUtil.isTokenValid(anyString(), any())).thenReturn(true);

        // BookModelAssembler depende de authorService.toSummary
        when(authorService.toSummary(any(Author.class))).thenReturn(
                AuthorDTO.Summary.builder().id(1L).name("Tolkien").nationality("British").build());
    }

    // ---- GET — public endpoints ----

    @Test
    @DisplayName("GET /api/v1/books - 200 without authentication (public)")
    void findAll_public() throws Exception {
        when(bookService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} - 200 with HATEOAS links")
    void findById_found() throws Exception {
        when(bookService.findById(1L)).thenReturn(book);

        mockMvc.perform(get("/api/v1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("The Hobbit"))
                .andExpect(jsonPath("$.author.name").value("Tolkien"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.author.href").exists())
                .andExpect(jsonPath("$._links.same-genre.href").exists());
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} - 404 when not found")
    void findById_notFound() throws Exception {
        when(bookService.findById(99L)).thenThrow(new ResourceNotFoundException("Book", 99L));

        mockMvc.perform(get("/api/v1/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/books/isbn/{isbn} - 200 with HATEOAS")
    void findByIsbn() throws Exception {
        when(bookService.findByIsbn("9780547928227")).thenReturn(book);

        mockMvc.perform(get("/api/v1/books/isbn/9780547928227"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9780547928227"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("GET /api/v1/books/available - 200 (public)")
    void findAvailable() throws Exception {
        when(bookService.findAvailable(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(book)));

        mockMvc.perform(get("/api/v1/books/available"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/books/author/{id} - 200 (public)")
    void findByAuthor() throws Exception {
        when(bookService.findByAuthor(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(book)));

        mockMvc.perform(get("/api/v1/books/author/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/books/search - 200 (public)")
    void search() throws Exception {
        when(bookService.search(eq("Hobbit"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(book)));

        mockMvc.perform(get("/api/v1/books/search?q=Hobbit"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/books/stats/genres - 200 (public)")
    void getGenreStats() throws Exception {
        when(bookService.getGenreStats()).thenReturn(List.of(
                BookDTO.GenreStats.builder().genre("Fantasy").count(5L)
                        .averagePrice(new BigDecimal("65.00")).build()));

        mockMvc.perform(get("/api/v1/books/stats/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genre").value("Fantasy"))
                .andExpect(jsonPath("$[0].count").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/books/top-expensive - 200 with CollectionModel envelope")
    void getTopExpensive() throws Exception {
        when(bookService.getTopExpensive(3)).thenReturn(List.of(book));

        mockMvc.perform(get("/api/v1/books/top-expensive?limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    // ---- POST ----

    @Test
    @DisplayName("POST /api/v1/books - 201 when authenticated + valid body")
    void create_valid() throws Exception {
        when(bookService.create(any())).thenReturn(book);

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.author.href").exists());
    }

    @Test
    @DisplayName("POST /api/v1/books - 400 for missing required fields")
    void create_invalid() throws Exception {
        BookDTO.Request bad = BookDTO.Request.builder()
                .title("").isbn(null).authorId(null).build();

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("POST /api/v1/books - 409 for duplicate ISBN")
    void create_duplicateIsbn() throws Exception {
        when(bookService.create(any())).thenThrow(new BusinessException("ISBN already exists"));

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict());
    }

    // ---- PUT ----

    @Test
    @DisplayName("PUT /api/v1/books/{id} - 200 with updated HATEOAS links")
    void update_valid() throws Exception {
        when(bookService.update(eq(1L), any())).thenReturn(book);

        mockMvc.perform(put("/api/v1/books/1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    // ---- PATCH ----

    @Test
    @DisplayName("PATCH /api/v1/books/{id}/availability - 200 toggled + links")
    void patchAvailability() throws Exception {
        Book unavailable = Book.builder()
                .id(1L).title("The Hobbit").isbn("9780547928227")
                .genre("Fantasy").price(new BigDecimal("49.90"))
                .available(false).author(author).build();
        when(bookService.patchAvailability(1L, false)).thenReturn(unavailable);

        mockMvc.perform(patch("/api/v1/books/1/availability")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("available", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    // ---- DELETE ----

    @Test
    @DisplayName("DELETE /api/v1/books/{id} - 204 for ADMIN")
    void delete_asAdmin() throws Exception {
        doNothing().when(bookService).delete(1L);

        mockMvc.perform(delete("/api/v1/books/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/books/{id} - 403 for USER")
    void delete_asUser_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/books/1")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
