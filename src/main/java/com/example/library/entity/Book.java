package com.example.library.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_book_isbn", columnList = "isbn", unique = true),
        @Index(name = "idx_book_author", columnList = "author_id"),
        @Index(name = "idx_book_genre", columnList = "genre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
            message = "Invalid ISBN format")
    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @Column(length = 60)
    private String genre;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Min(value = 1, message = "Pages must be positive")
    @Column(name = "pages")
    private Integer pages;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 8, fraction = 2)
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (available == null) available = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
