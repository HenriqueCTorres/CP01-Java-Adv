package com.example.library.config;

import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.entity.User;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedAuthorsAndBooks();
        log.info("==> Data initialization complete. Users: {}, Authors: {}, Books: {}",
                userRepository.count(), authorRepository.count(), bookRepository.count());
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .build());
        }
        if (!userRepository.existsByUsername("user")) {
            userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .role(User.Role.USER)
                    .build());
        }
    }

    private void seedAuthorsAndBooks() {
        if (authorRepository.count() > 0) return;

        Author machado = authorRepository.save(Author.builder()
                .name("Machado de Assis")
                .email("machado@example.com")
                .nationality("Brazilian")
                .biography("Considered the greatest Brazilian writer of all time.")
                .build());

        Author clarice = authorRepository.save(Author.builder()
                .name("Clarice Lispector")
                .email("clarice@example.com")
                .nationality("Brazilian")
                .biography("One of the most significant Latin American authors of the 20th century.")
                .build());

        Author tolkien = authorRepository.save(Author.builder()
                .name("J.R.R. Tolkien")
                .email("tolkien@example.com")
                .nationality("British")
                .biography("Author of The Lord of the Rings and The Hobbit.")
                .build());

        bookRepository.save(Book.builder()
                .title("Dom Casmurro")
                .isbn("9788535902778")
                .genre("Romance")
                .description("A classic Brazilian novel exploring jealousy and betrayal.")
                .publishedDate(LocalDate.of(1899, 1, 1))
                .pages(256)
                .price(new BigDecimal("35.90"))
                .available(true)
                .author(machado)
                .build());

        bookRepository.save(Book.builder()
                .title("Memórias Póstumas de Brás Cubas")
                .isbn("9788535902785")
                .genre("Romance")
                .description("A postmodern novel narrated by a dead man.")
                .publishedDate(LocalDate.of(1881, 1, 1))
                .pages(200)
                .price(new BigDecimal("29.90"))
                .available(true)
                .author(machado)
                .build());

        bookRepository.save(Book.builder()
                .title("A Hora da Estrela")
                .isbn("9788532511959")
                .genre("Literary Fiction")
                .description("A meditation on the existence of an ordinary woman.")
                .publishedDate(LocalDate.of(1977, 1, 1))
                .pages(96)
                .price(new BigDecimal("39.90"))
                .available(true)
                .author(clarice)
                .build());

        bookRepository.save(Book.builder()
                .title("The Lord of the Rings")
                .isbn("9780618640157")
                .genre("Fantasy")
                .description("An epic high-fantasy novel set in Middle-earth.")
                .publishedDate(LocalDate.of(1954, 7, 29))
                .pages(1178)
                .price(new BigDecimal("89.90"))
                .available(true)
                .author(tolkien)
                .build());

        bookRepository.save(Book.builder()
                .title("The Hobbit")
                .isbn("9780547928227")
                .genre("Fantasy")
                .description("The prequel to The Lord of the Rings.")
                .publishedDate(LocalDate.of(1937, 9, 21))
                .pages(310)
                .price(new BigDecimal("49.90"))
                .available(false)
                .author(tolkien)
                .build());
    }
}
