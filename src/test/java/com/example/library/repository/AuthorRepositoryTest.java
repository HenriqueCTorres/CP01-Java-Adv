package com.example.library.repository;

import com.example.library.entity.Author;
import com.example.library.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AuthorRepository Integration Tests")
class AuthorRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EntityManager em;

    private Author machado;
    private Author clarice;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();

        machado = authorRepository.save(Author.builder()
                .name("Machado de Assis")
                .email("machado@example.com")
                .nationality("Brazilian")
                .biography("Greatest Brazilian writer")
                .build());

        clarice = authorRepository.save(Author.builder()
                .name("Clarice Lispector")
                .email("clarice@example.com")
                .nationality("Brazilian")
                .biography("Modernist master")
                .build());
    }

    @Test
    @DisplayName("Should find author by email")
    void findByEmail() {
        Optional<Author> found = authorRepository.findByEmail("machado@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Machado de Assis");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_notFound() {
        Optional<Author> found = authorRepository.findByEmail("nobody@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check email existence")
    void existsByEmail() {
        assertThat(authorRepository.existsByEmail("machado@example.com")).isTrue();
        assertThat(authorRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should search by name case-insensitively")
    void searchByName() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Author> page = authorRepository.searchByName("machado", pageable);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Machado de Assis");
    }

    @Test
    @DisplayName("Should return all results when search term matches multiple")
    void searchByName_multiple() {
        Page<Author> page = authorRepository.searchByName("a", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2); // Mach"a"do, Cl"a"rice
    }

    @Test
    @DisplayName("Should find authors by nationality")
    void findByNationality() {
        Page<Author> page = authorRepository.findByNationalityIgnoreCase("brazilian",
                PageRequest.of(0, 10, Sort.by("name")));
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find author with books eagerly")
    void findByIdWithBooks() {
        bookRepository.save(Book.builder()
                .title("Dom Casmurro").isbn("9788535902778")
                .genre("Romance").price(BigDecimal.TEN)
                .available(true).author(machado)
                .build());

        // Força o flush para o banco e limpa o cache de primeiro nível,
        // garantindo que o JOIN FETCH releia de fato o banco
        em.flush();
        em.clear();

        Optional<Author> found = authorRepository.findByIdWithBooks(machado.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getBooks()).hasSize(1);
    }

    @Test
    @DisplayName("Should find authors with at least N books")
    void findAuthorsWithAtLeastNBooks() {
        bookRepository.save(Book.builder()
                .title("Dom Casmurro").isbn("9788535902778")
                .genre("Romance").price(BigDecimal.TEN)
                .available(true).author(machado)
                .build());

        em.flush();
        em.clear();

        List<Author> result = authorRepository.findAuthorsWithAtLeastNBooks(1);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(machado.getId());
    }

    @Test
    @DisplayName("Should paginate results correctly")
    void pagination() {
        Page<Author> page1 = authorRepository.findAll(PageRequest.of(0, 1, Sort.by("name")));
        Page<Author> page2 = authorRepository.findAll(PageRequest.of(1, 1, Sort.by("name")));

        assertThat(page1.getContent()).hasSize(1);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page1.getContent().get(0).getId())
                .isNotEqualTo(page2.getContent().get(0).getId());
        assertThat(page1.getTotalElements()).isEqualTo(2);
        assertThat(page1.getTotalPages()).isEqualTo(2);
    }
}
