package com.example.library.controller;

import com.example.library.dto.AuthorDTO;
import com.example.library.dto.BookDTO;
import com.example.library.entity.Book;
import com.example.library.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
@RequiredArgsConstructor
public class BookModelAssembler
        implements RepresentationModelAssembler<Book, EntityModel<BookDTO.Response>> {

    private final AuthorService authorService;

    @Override
    @NonNull
    public EntityModel<BookDTO.Response> toModel(@NonNull Book book) {
        AuthorDTO.Summary authorSummary = authorService.toSummary(book.getAuthor());

        BookDTO.Response response = BookDTO.Response.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .genre(book.getGenre())
                .publishedDate(book.getPublishedDate())
                .pages(book.getPages())
                .price(book.getPrice())
                .available(book.getAvailable())
                .author(authorSummary)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();

        response.add(linkTo(methodOn(BookController.class).findById(book.getId())).withSelfRel());
        response.add(linkTo(methodOn(BookController.class).findAll(Pageable.unpaged())).withRel("books"));
        response.add(linkTo(methodOn(AuthorController.class).findById(book.getAuthor().getId())).withRel("author"));

        if (book.getGenre() != null) {
            response.add(linkTo(methodOn(BookController.class)
                    .findByGenre(book.getGenre(), Pageable.unpaged())).withRel("same-genre"));
        }

        return EntityModel.of(response);
    }

    /** Conveniência para endpoints que retornam BookDTO.Response direto (sem PagedModel). */
    public BookDTO.Response toResponse(Book book) {
        return toModel(book).getContent();
    }
}
