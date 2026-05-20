package com.example.library.service;

import com.example.library.dto.AuthorDTO;
import com.example.library.entity.Author;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorService Unit Tests")
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    private Author author;
    private AuthorDTO.Request request;

    @BeforeEach
    void setUp() {
        author = Author.builder()
                .id(1L)
                .name("Machado de Assis")
                .email("machado@example.com")
                .nationality("Brazilian")
                .biography("Greatest Brazilian writer")
                .books(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        request = AuthorDTO.Request.builder()
                .name("Machado de Assis")
                .email("machado@example.com")
                .nationality("Brazilian")
                .biography("Greatest Brazilian writer")
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return author when found")
        void shouldReturnAuthor() {
            when(authorRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(author));
            Author found = authorService.findById(1L);
            assertThat(found).isEqualTo(author);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(authorRepository.findByIdWithBooks(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authorService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Should return paginated authors")
        void shouldReturnPage() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
            Page<Author> page = new PageImpl<>(List.of(author), pageable, 1);
            when(authorRepository.findAll(pageable)).thenReturn(page);

            Page<Author> result = authorService.findAll(pageable);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Should create author successfully")
        void shouldCreate() {
            when(authorRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(authorRepository.save(any(Author.class))).thenReturn(author);

            Author created = authorService.create(request);
            assertThat(created.getName()).isEqualTo("Machado de Assis");
            verify(authorRepository).save(any(Author.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when email already exists")
        void shouldThrowOnDuplicateEmail() {
            when(authorRepository.existsByEmail(request.getEmail())).thenReturn(true);
            assertThatThrownBy(() -> authorService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
            verify(authorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Should update author successfully")
        void shouldUpdate() {
            when(authorRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(author));
            when(authorRepository.save(any(Author.class))).thenReturn(author);

            AuthorDTO.Request updateRequest = AuthorDTO.Request.builder()
                    .name("Machado de Assis Updated")
                    .email("machado@example.com")
                    .build();

            Author updated = authorService.update(1L, updateRequest);
            assertThat(updated).isNotNull();
            verify(authorRepository).save(author);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Should delete author with no books")
        void shouldDelete() {
            when(authorRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(author));
            authorService.delete(1L);
            verify(authorRepository).delete(author);
        }

        @Test
        @DisplayName("Should throw when author has books")
        void shouldThrowWhenHasBooks() {
            author.getBooks().add(new com.example.library.entity.Book());
            when(authorRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(author));

            assertThatThrownBy(() -> authorService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot delete author");

            verify(authorRepository, never()).delete(any());
        }
    }

    @Test
    @DisplayName("Should search by name with pagination")
    void shouldSearchByName() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Author> page = new PageImpl<>(List.of(author));
        when(authorRepository.searchByName("Machado", pageable)).thenReturn(page);

        Page<Author> result = authorService.searchByName("Machado", pageable);
        assertThat(result.getContent()).hasSize(1);
    }
}
