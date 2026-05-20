package com.example.library.repository;

import com.example.library.entity.Author;
import com.example.library.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BookRepository Integration Tests")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    private Author author;
    private Book hobbit;
    private Book lotr;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();

        author = authorRepository.save(Author.builder()
                .name("J.R.R. Tolkien").nationality("British")
                .email("tolkien@example.com").build());

        hobbit = bookRepository.save(Book.builder()
                .title("The Hobbit").isbn("9780547928227")
                .genre("Fantasy").price(new BigDecimal("49.90"))
                .pages(310).available(false)
                .publishedDate(LocalDate.of(1937, 9, 21))
                .author(author).build());

        lotr = bookRepository.save(Book.builder()
                .title("The Lord of the Rings").isbn("9780618640157")
                .genre("Fantasy").price(new BigDecimal("89.90"))
                .pages(1178).available(true)
                .publishedDate(LocalDate.of(1954, 7, 29))
                .description("Epic fantasy in Middle-earth")
                .author(author).build());
    }

    @Test
    @DisplayName("Should find book by ISBN")
    void findByIsbn() {
        Optional<Book> found = bookRepository.findByIsbn("9780547928227");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("The Hobbit");
    }

    @Test
    @DisplayName("Should check ISBN existence")
    void existsByIsbn() {
        assertThat(bookRepository.existsByIsbn("9780547928227")).isTrue();
        assertThat(bookRepository.existsByIsbn("0000000000000")).isFalse();
    }

    @Test
    @DisplayName("Should find only available books")
    void findByAvailableTrue() {
        Page<Book> page = bookRepository.findByAvailableTrue(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("The Lord of the Rings");
    }

    @Test
    @DisplayName("Should find books by author ID")
    void findByAuthorId() {
        Page<Book> page = bookRepository.findByAuthorId(author.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find books by genre (case-insensitive)")
    void findByGenreIgnoreCase() {
        Page<Book> page = bookRepository.findByGenreIgnoreCase("fantasy", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should search by title or description")
    void searchByTitleOrDescription() {
        Page<Book> byTitle = bookRepository.searchByTitleOrDescription("hobbit", PageRequest.of(0, 10));
        assertThat(byTitle.getContent()).hasSize(1);

        Page<Book> byDesc = bookRepository.searchByTitleOrDescription("Middle-earth", PageRequest.of(0, 10));
        assertThat(byDesc.getContent()).hasSize(1);
        assertThat(byDesc.getContent().get(0).getTitle()).isEqualTo("The Lord of the Rings");
    }

    @Test
    @DisplayName("Should find books in price range")
    void findByPriceRange() {
        Page<Book> page = bookRepository.findByPriceRange(
                new BigDecimal("40"), new BigDecimal("60"),
                PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("The Hobbit");
    }

    @Test
    @DisplayName("Should use Specification for dynamic filtering")
    void filterWithSpecification() {
        Specification<Book> spec = BookSpecification.withFilters(
                null, "Fantasy", null, null, null, true);

        Page<Book> page = bookRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should return genre statistics")
    void getStatsByGenre() {
        List<Object[]> stats = bookRepository.getStatsByGenre();
        assertThat(stats).hasSize(1);
        assertThat(stats.get(0)[0]).isEqualTo("Fantasy");
        assertThat(stats.get(0)[1]).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should return top expensive books")
    void findTopByPrice() {
        List<Book> top = bookRepository.findTopByPrice(PageRequest.of(0, 1));
        assertThat(top).hasSize(1);
        assertThat(top.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("89.90"));
    }

    @Test
    @DisplayName("Should sort results correctly")
    void sorting() {
        Page<Book> asc = bookRepository.findAll(PageRequest.of(0, 10, Sort.by("price").ascending()));
        assertThat(asc.getContent().get(0).getTitle()).isEqualTo("The Hobbit");

        Page<Book> desc = bookRepository.findAll(PageRequest.of(0, 10, Sort.by("price").descending()));
        assertThat(desc.getContent().get(0).getTitle()).isEqualTo("The Lord of the Rings");
    }
}
