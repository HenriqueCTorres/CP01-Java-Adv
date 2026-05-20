package com.example.library.controller;

import com.example.library.dto.BookDTO;
import com.example.library.entity.Book;
import com.example.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Book management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BookController {

    private final BookService bookService;
    private final BookModelAssembler assembler;
    private final PagedResourcesAssembler<Book> pagedAssembler;

    @GetMapping
    @Operation(summary = "List all books with pagination and sorting")
    public ResponseEntity<PagedModel<EntityModel<BookDTO.Response>>> findAll(
            @PageableDefault(size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(pagedAssembler.toModel(bookService.findAll(pageable), assembler));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<EntityModel<BookDTO.Response>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(bookService.findById(id)));
    }

    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "Get book by ISBN")
    public ResponseEntity<EntityModel<BookDTO.Response>> findByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(assembler.toModel(bookService.findByIsbn(isbn)));
    }

    @GetMapping("/available")
    @Operation(summary = "List available books")
    public ResponseEntity<PagedModel<EntityModel<BookDTO.Response>>> findAvailable(
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(pagedAssembler.toModel(bookService.findAvailable(pageable), assembler));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "List books by author")
    public ResponseEntity<PagedModel<EntityModel<BookDTO.Response>>> findByAuthor(
            @Parameter(description = "Author ID") @PathVariable Long authorId,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(pagedAssembler.toModel(bookService.findByAuthor(authorId, pageable), assembler));
    }

    @GetMapping("/genre/{genre}")
    @Operation(summary = "List books by genre")
    public ResponseEntity<PagedModel<EntityModel<BookDTO.Response>>> findByGenre(
            @PathVariable String genre,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(pagedAssembler.toModel(bookService.findByGenre(genre, pageable), assembler));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search on title and description")
    public ResponseEntity<PagedModel<EntityModel<BookDTO.Response>>> search(
            @RequestParam String q,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(pagedAssembler.toModel(bookService.search(q, pageable), assembler));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter books using multiple criteria (Specifications)")
    public ResponseEntity<PagedModel<EntityModel<BookDTO.Response>>> filter(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {

        BookDTO.SearchRequest filter = BookDTO.SearchRequest.builder()
                .title(title).genre(genre).authorName(authorName)
                .minPrice(minPrice).maxPrice(maxPrice).available(available)
                .build();
        return ResponseEntity.ok(pagedAssembler.toModel(bookService.filter(filter, pageable), assembler));
    }

    @GetMapping("/price-range")
    @Operation(summary = "Find books within a price range")
    public ResponseEntity<PagedModel<EntityModel<BookDTO.Response>>> findByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max,
            @PageableDefault(size = 10, sort = "price") Pageable pageable) {

        return ResponseEntity.ok(pagedAssembler.toModel(bookService.findByPriceRange(min, max, pageable), assembler));
    }

    @GetMapping("/stats/genres")
    @Operation(summary = "Get book count and average price per genre (cached)")
    public ResponseEntity<List<BookDTO.GenreStats>> getGenreStats() {
        return ResponseEntity.ok(bookService.getGenreStats());
    }

    @GetMapping("/top-expensive")
    @Operation(summary = "Get the most expensive books")
    public ResponseEntity<CollectionModel<EntityModel<BookDTO.Response>>> getTopExpensive(
            @RequestParam(defaultValue = "5") int limit) {
        List<EntityModel<BookDTO.Response>> list = bookService.getTopExpensive(limit)
                .stream().map(assembler::toModel).toList();
        CollectionModel<EntityModel<BookDTO.Response>> model = CollectionModel.of(list,
                linkTo(methodOn(BookController.class).getTopExpensive(limit)).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @PostMapping
    @Operation(summary = "Create a new book")
    public ResponseEntity<EntityModel<BookDTO.Response>> create(
            @Valid @RequestBody BookDTO.Request request) {
        Book book = bookService.create(request);
        EntityModel<BookDTO.Response> model = assembler.toModel(book);
        return ResponseEntity.created(URI.create("/api/v1/books/" + book.getId()))
                .body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a book")
    public ResponseEntity<EntityModel<BookDTO.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody BookDTO.Request request) {
        return ResponseEntity.ok(assembler.toModel(bookService.update(id, request)));
    }

    @PatchMapping("/{id}/availability")
    @Operation(summary = "Toggle book availability")
    public ResponseEntity<EntityModel<BookDTO.Response>> patchAvailability(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean available = body.getOrDefault("available", true);
        return ResponseEntity.ok(assembler.toModel(bookService.patchAvailability(id, available)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book")
    @ApiResponse(responseCode = "204", description = "Book deleted")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
