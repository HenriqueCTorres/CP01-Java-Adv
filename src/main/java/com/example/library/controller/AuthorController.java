package com.example.library.controller;

import com.example.library.dto.AuthorDTO;
import com.example.library.entity.Author;
import com.example.library.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
@Tag(name = "Authors", description = "Author management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AuthorController {

    private final AuthorService authorService;
    private final AuthorModelAssembler assembler;
    private final PagedResourcesAssembler<Author> pagedAssembler;

    @GetMapping
    @Operation(summary = "List all authors", description = "Returns a paginated list of authors")
    @ApiResponse(responseCode = "200", description = "Authors retrieved successfully")
    public ResponseEntity<PagedModel<EntityModel<AuthorDTO.Response>>> findAll(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<Author> page = authorService.findAll(pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page, assembler));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get author by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author found"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    public ResponseEntity<EntityModel<AuthorDTO.Response>> findById(
            @Parameter(description = "Author ID") @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(authorService.findById(id)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search authors by name")
    public ResponseEntity<PagedModel<EntityModel<AuthorDTO.Response>>> search(
            @RequestParam String name,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<Author> page = authorService.searchByName(name, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page, assembler));
    }

    @GetMapping("/nationality/{nationality}")
    @Operation(summary = "Find authors by nationality")
    public ResponseEntity<PagedModel<EntityModel<AuthorDTO.Response>>> findByNationality(
            @PathVariable String nationality,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<Author> page = authorService.findByNationality(nationality, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page, assembler));
    }

    @GetMapping("/prolific")
    @Operation(summary = "Find authors with at least N books")
    public ResponseEntity<CollectionModel<EntityModel<AuthorDTO.Response>>> findProlific(
            @RequestParam(defaultValue = "1") int minBooks) {

        List<EntityModel<AuthorDTO.Response>> list = authorService
                .findAuthorsWithAtLeastNBooks(minBooks)
                .stream().map(assembler::toModel).toList();
        CollectionModel<EntityModel<AuthorDTO.Response>> model = CollectionModel.of(list,
                linkTo(methodOn(AuthorController.class).findProlific(minBooks)).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @PostMapping
    @Operation(summary = "Create a new author")
    @ApiResponse(responseCode = "201", description = "Author created")
    public ResponseEntity<EntityModel<AuthorDTO.Response>> create(
            @Valid @RequestBody AuthorDTO.Request request) {
        Author author = authorService.create(request);
        EntityModel<AuthorDTO.Response> model = assembler.toModel(author);
        return ResponseEntity.created(URI.create("/api/v1/authors/" + author.getId()))
                .body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an author")
    public ResponseEntity<EntityModel<AuthorDTO.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody AuthorDTO.Request request) {
        return ResponseEntity.ok(assembler.toModel(authorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an author")
    @ApiResponse(responseCode = "204", description = "Author deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
