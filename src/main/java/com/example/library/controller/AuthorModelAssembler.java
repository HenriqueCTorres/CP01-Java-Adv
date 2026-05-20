package com.example.library.controller;

import com.example.library.dto.AuthorDTO;
import com.example.library.entity.Author;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class AuthorModelAssembler
        implements RepresentationModelAssembler<Author, EntityModel<AuthorDTO.Response>> {

    @Override
    @NonNull
    public EntityModel<AuthorDTO.Response> toModel(@NonNull Author author) {
        AuthorDTO.Response response = AuthorDTO.Response.builder()
                .id(author.getId())
                .name(author.getName())
                .email(author.getEmail())
                .biography(author.getBiography())
                .nationality(author.getNationality())
                .bookCount(author.getBooks() != null ? author.getBooks().size() : null)
                .createdAt(author.getCreatedAt())
                .updatedAt(author.getUpdatedAt())
                .build();

        response.add(linkTo(methodOn(AuthorController.class).findById(author.getId())).withSelfRel());
        response.add(linkTo(methodOn(AuthorController.class).findAll(Pageable.unpaged())).withRel("authors"));
        response.add(linkTo(methodOn(BookController.class).findByAuthor(author.getId(), Pageable.unpaged())).withRel("books"));

        return EntityModel.of(response);
    }
}
