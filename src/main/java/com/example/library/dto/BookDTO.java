package com.example.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookDTO {

    @Schema(description = "Request payload to create or update a book")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 200)
        @Schema(description = "Book title", example = "Dom Casmurro")
        private String title;

        @NotBlank(message = "ISBN is required")
        @Schema(description = "ISBN-10 or ISBN-13", example = "978-3-16-148410-0")
        private String isbn;

        @Size(max = 1000)
        @Schema(description = "Book description")
        private String description;

        @Schema(description = "Literary genre", example = "Romance")
        private String genre;

        @Schema(description = "Publication date", example = "1899-01-01")
        private LocalDate publishedDate;

        @Min(1)
        @Schema(description = "Number of pages", example = "256")
        private Integer pages;

        @DecimalMin("0.0")
        @Schema(description = "Price in BRL", example = "49.90")
        private BigDecimal price;

        @Schema(description = "Is the book available?", example = "true")
        private Boolean available;

        @NotNull(message = "Author ID is required")
        @Schema(description = "Author ID", example = "1")
        private Long authorId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Relation(collectionRelation = "books", itemRelation = "book")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Book response with HATEOAS links")
    public static class Response extends RepresentationModel<Response> {

        private Long id;
        private String title;
        private String isbn;
        private String description;
        private String genre;
        private LocalDate publishedDate;
        private Integer pages;
        private BigDecimal price;
        private Boolean available;
        private AuthorDTO.Summary author;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Book search/filter parameters")
    public static class SearchRequest {
        private String title;
        private String genre;
        private String authorName;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Boolean available;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Genre statistics")
    public static class GenreStats {
        private String genre;
        private Long count;
        private BigDecimal averagePrice;
    }
}
