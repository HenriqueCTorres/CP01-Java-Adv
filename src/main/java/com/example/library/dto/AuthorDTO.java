package com.example.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

public class AuthorDTO {

    @Schema(description = "Request payload to create or update an author")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100)
        @Schema(description = "Author full name", example = "Machado de Assis")
        private String name;

        @Email(message = "Invalid email")
        @Schema(description = "Author email", example = "machado@example.com")
        private String email;

        @Size(max = 500)
        @Schema(description = "Short biography")
        private String biography;

        @Schema(description = "Nationality", example = "Brazilian")
        private String nationality;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Relation(collectionRelation = "authors", itemRelation = "author")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Author response with HATEOAS links")
    public static class Response extends RepresentationModel<Response> {

        @Schema(description = "Author ID", example = "1")
        private Long id;

        @Schema(description = "Author full name")
        private String name;

        @Schema(description = "Author email")
        private String email;

        @Schema(description = "Short biography")
        private String biography;

        @Schema(description = "Nationality")
        private String nationality;

        @Schema(description = "Number of books")
        private Integer bookCount;

        @Schema(description = "Creation timestamp")
        private LocalDateTime createdAt;

        @Schema(description = "Last update timestamp")
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Author summary (used in book responses)")
    public static class Summary {
        private Long id;
        private String name;
        private String nationality;
    }
}
