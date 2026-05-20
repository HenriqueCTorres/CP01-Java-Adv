package com.example.library.service;

import com.example.library.dto.BookDTO;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    private Book book;
    private Author author;
    private BookDTO.Request request;

    @BeforeEach
    void setUp() {
        author = Author.builder()
                .id(1L).name("Tolkien").nationality("British").books(new ArrayList<>())
                .build();

        book = Book.builder()
                .id(1L).title("The Hobbit").isbn("9780547928227")
                .genre("Fantasy").price(new BigDecimal("49.90"))
                .available(true).author(author)
                .build();

        request = BookDTO.Request.builder()
                .title("The Hobbit").isbn("9780547928227")
                .genre("Fantasy").price(new BigDecimal("49.90"))
                .available(true).authorId(1L)
                .build();
    }

    @Test
    @DisplayName("findById - should return book when found")
    void findById_found() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        assertThat(bookService.findById(1L)).isEqualTo(book);
    }

    @Test
    @DisplayName("findById - should throw when not found")
    void findById_notFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findByIsbn - should return book")
    void findByIsbn() {
        when(bookRepository.findByIsbn("9780547928227")).thenReturn(Optional.of(book));
        assertThat(bookService.findByIsbn("9780547928227")).isEqualTo(book);
    }

    @Test
    @DisplayName("create - should create book")
    void create_success() {
        when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        Book created = bookService.create(request);
        assertThat(created).isNotNull();
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("create - should throw on duplicate ISBN")
    void create_duplicateIsbn() {
        when(bookRepository.existsByIsbn("9780547928227")).thenReturn(true);
        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ISBN");
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - should throw when author not found")
    void create_authorNotFound() {
        when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete - should delete existing book")
    void delete_success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        bookService.delete(1L);
        verify(bookRepository).delete(book);
    }

    @Test
    @DisplayName("patchAvailability - should toggle availability")
    void patchAvailability() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.patchAvailability(1L, false);
        assertThat(result.getAvailable()).isFalse();
    }

    @Test
    @DisplayName("findAll - should return paginated results")
    void findAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> page = new PageImpl<>(List.of(book), pageable, 1);
        when(bookRepository.findAll(pageable)).thenReturn(page);

        Page<Book> result = bookService.findAll(pageable);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getGenreStats - should return stats")
    void getGenreStats() {
        Object[] row = {"Fantasy", 5L, 59.90};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(bookRepository.getStatsByGenre()).thenReturn(rows);

        List<BookDTO.GenreStats> stats = bookService.getGenreStats();
        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).getGenre()).isEqualTo("Fantasy");
        assertThat(stats.get(0).getCount()).isEqualTo(5L);
    }
}
