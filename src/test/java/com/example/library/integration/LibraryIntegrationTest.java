package com.example.library.integration;

import com.example.library.dto.AuthDTO;
import com.example.library.dto.AuthorDTO;
import com.example.library.dto.BookDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Full Integration Tests")
class LibraryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Shared state across tests
    static String adminToken;
    static String userToken;
    static Long authorId;
    static Long bookId;

    // ============================================================
    // Authentication
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("Should login as admin and receive JWT")
    void loginAsAdmin() throws Exception {
        AuthDTO.LoginRequest req = new AuthDTO.LoginRequest("admin", "admin123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        AuthDTO.TokenResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthDTO.TokenResponse.class);
        adminToken = response.getToken();
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("Should register a new user and receive JWT")
    void registerUser() throws Exception {
        AuthDTO.RegisterRequest req = new AuthDTO.RegisterRequest("newuser", "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        AuthDTO.TokenResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthDTO.TokenResponse.class);
        userToken = response.getToken();
        assertThat(userToken).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("Should reject duplicate registration")
    void registerDuplicate() throws Exception {
        AuthDTO.RegisterRequest req = new AuthDTO.RegisterRequest("admin", "anything");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    @DisplayName("Should reject invalid credentials")
    void loginWrongPassword() throws Exception {
        AuthDTO.LoginRequest req = new AuthDTO.LoginRequest("admin", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // Authors
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("Should create an author as authenticated user")
    void createAuthor() throws Exception {
        AuthorDTO.Request req = AuthorDTO.Request.builder()
                .name("Gabriel García Márquez")
                .email("garcia@example.com")
                .nationality("Colombian")
                .biography("Nobel Prize winner")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/authors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Gabriel García Márquez"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.books.href").exists())
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        authorId = node.get("id").asLong();
        assertThat(authorId).isPositive();
    }

    @Test
    @Order(11)
    @DisplayName("Should reject creating author without authentication")
    void createAuthor_unauthenticated() throws Exception {
        AuthorDTO.Request req = AuthorDTO.Request.builder().name("Test Author").build();

        mockMvc.perform(post("/api/v1/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("Should get author by ID with HATEOAS links")
    void getAuthorById() throws Exception {
        mockMvc.perform(get("/api/v1/authors/" + authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(authorId))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.books").exists());
    }

    @Test
    @Order(13)
    @DisplayName("Should list authors with pagination")
    void listAuthors() throws Exception {
        mockMvc.perform(get("/api/v1/authors?page=0&size=5&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.size").value(5));
    }

    @Test
    @Order(14)
    @DisplayName("Should search authors by name")
    void searchAuthors() throws Exception {
        mockMvc.perform(get("/api/v1/authors/search?name=García"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(15)
    @DisplayName("Should update author")
    void updateAuthor() throws Exception {
        AuthorDTO.Request req = AuthorDTO.Request.builder()
                .name("Gabriel García Márquez (Updated)")
                .email("garcia_updated@example.com")
                .nationality("Colombian")
                .build();

        mockMvc.perform(put("/api/v1/authors/" + authorId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gabriel García Márquez (Updated)"));
    }

    // ============================================================
    // Books
    // ============================================================

    @Test
    @Order(20)
    @DisplayName("Should create a book")
    void createBook() throws Exception {
        BookDTO.Request req = BookDTO.Request.builder()
                .title("One Hundred Years of Solitude")
                .isbn("9780060883287")
                .genre("Magical Realism")
                .description("A multigenerational story of the Buendía family")
                .publishedDate(LocalDate.of(1967, 5, 30))
                .pages(417)
                .price(new BigDecimal("59.90"))
                .available(true)
                .authorId(authorId)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("One Hundred Years of Solitude"))
                .andExpect(jsonPath("$.author.id").value(authorId))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.author").exists())
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        bookId = node.get("id").asLong();
        assertThat(bookId).isPositive();
    }

    @Test
    @Order(21)
    @DisplayName("Should get book by ID with HATEOAS")
    void getBookById() throws Exception {
        mockMvc.perform(get("/api/v1/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId))
                .andExpect(jsonPath("$._links").exists());
    }

    @Test
    @Order(22)
    @DisplayName("Should filter books by genre")
    void filterBooksByGenre() throws Exception {
        mockMvc.perform(get("/api/v1/books/genre/Magical Realism"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(23)
    @DisplayName("Should filter books with Specifications")
    void filterBooks() throws Exception {
        mockMvc.perform(get("/api/v1/books/filter?genre=Magical Realism&available=true"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(24)
    @DisplayName("Should toggle book availability via PATCH")
    void patchBookAvailability() throws Exception {
        mockMvc.perform(patch("/api/v1/books/" + bookId + "/availability")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @Order(25)
    @DisplayName("Should get genre statistics (uses cache)")
    void getGenreStats() throws Exception {
        // First call - populates cache
        mockMvc.perform(get("/api/v1/books/stats/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Second call - served from cache
        mockMvc.perform(get("/api/v1/books/stats/genres"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(26)
    @DisplayName("Should search books by text")
    void searchBooks() throws Exception {
        mockMvc.perform(get("/api/v1/books/search?q=Solitude"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(27)
    @DisplayName("Should find books by author")
    void findBooksByAuthor() throws Exception {
        mockMvc.perform(get("/api/v1/books/author/" + authorId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(28)
    @DisplayName("Should return 409 for duplicate ISBN")
    void createBook_duplicateIsbn() throws Exception {
        BookDTO.Request req = BookDTO.Request.builder()
                .title("Another Book")
                .isbn("9780060883287") // same ISBN
                .authorId(authorId)
                .build();

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ============================================================
    // Delete (last - order matters)
    // ============================================================

    @Test
    @Order(90)
    @DisplayName("Should delete book as ADMIN")
    void deleteBook() throws Exception {
        mockMvc.perform(delete("/api/v1/books/" + bookId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(91)
    @DisplayName("Should delete author as ADMIN after books removed")
    void deleteAuthor() throws Exception {
        mockMvc.perform(delete("/api/v1/authors/" + authorId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(92)
    @DisplayName("USER should not be able to delete authors")
    void deleteAuthor_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/authors/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
