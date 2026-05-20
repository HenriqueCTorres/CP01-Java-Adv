package com.example.library.service;

import com.example.library.config.CacheConfig;
import com.example.library.dto.BookDTO;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BookSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Cacheable(value = CacheConfig.BOOKS_CACHE, key = "#id")
    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    public Book findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ISBN: " + isbn));
    }

    public Page<Book> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    public Page<Book> findAvailable(Pageable pageable) {
        return bookRepository.findByAvailableTrue(pageable);
    }

    public Page<Book> findByAuthor(Long authorId, Pageable pageable) {
        return bookRepository.findByAuthorId(authorId, pageable);
    }

    public Page<Book> findByGenre(String genre, Pageable pageable) {
        return bookRepository.findByGenreIgnoreCase(genre, pageable);
    }

    public Page<Book> search(String query, Pageable pageable) {
        return bookRepository.searchByTitleOrDescription(query, pageable);
    }

    public Page<Book> findByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable) {
        return bookRepository.findByPriceRange(min, max, pageable);
    }

    // Dynamic filter with Specifications
    public Page<Book> filter(BookDTO.SearchRequest filter, Pageable pageable) {
        var spec = BookSpecification.withFilters(
                filter.getTitle(),
                filter.getGenre(),
                filter.getAuthorName(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getAvailable());
        return bookRepository.findAll(spec, pageable);
    }

    @Cacheable(value = CacheConfig.GENRE_STATS_CACHE, key = "'all'")
    public List<BookDTO.GenreStats> getGenreStats() {
        return bookRepository.getStatsByGenre().stream()
                .map(row -> BookDTO.GenreStats.builder()
                        .genre((String) row[0])
                        .count((Long) row[1])
                        .averagePrice(row[2] != null ? BigDecimal.valueOf((Double) row[2]) : null)
                        .build())
                .collect(Collectors.toList());
    }

    public List<Book> getTopExpensive(int limit) {
        return bookRepository.findTopByPrice(PageRequest.of(0, limit));
    }

    @Transactional
    @CachePut(value = CacheConfig.BOOKS_CACHE, key = "#result.id")
    @CacheEvict(value = CacheConfig.GENRE_STATS_CACHE, allEntries = true)
    public Book create(BookDTO.Request request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("ISBN '" + request.getIsbn() + "' is already in use");
        }

        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author", request.getAuthorId()));

        Book book = Book.builder()
                .title(request.getTitle())
                .isbn(request.getIsbn())
                .description(request.getDescription())
                .genre(request.getGenre())
                .publishedDate(request.getPublishedDate())
                .pages(request.getPages())
                .price(request.getPrice())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .author(author)
                .build();

        Book saved = bookRepository.save(book);
        log.info("Created book id={}, isbn={}", saved.getId(), saved.getIsbn());
        return saved;
    }

    @Transactional
    @CachePut(value = CacheConfig.BOOKS_CACHE, key = "#id")
    @CacheEvict(value = CacheConfig.GENRE_STATS_CACHE, allEntries = true)
    public Book update(Long id, BookDTO.Request request) {
        Book book = findById(id);

        if (!book.getIsbn().equals(request.getIsbn()) && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("ISBN '" + request.getIsbn() + "' is already in use");
        }

        if (!book.getAuthor().getId().equals(request.getAuthorId())) {
            Author author = authorRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author", request.getAuthorId()));
            book.setAuthor(author);
        }

        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setDescription(request.getDescription());
        book.setGenre(request.getGenre());
        book.setPublishedDate(request.getPublishedDate());
        book.setPages(request.getPages());
        book.setPrice(request.getPrice());
        if (request.getAvailable() != null) book.setAvailable(request.getAvailable());

        Book saved = bookRepository.save(book);
        log.info("Updated book id={}", id);
        return saved;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.BOOKS_CACHE, key = "#id"),
            @CacheEvict(value = CacheConfig.GENRE_STATS_CACHE, allEntries = true)
    })
    public void delete(Long id) {
        Book book = findById(id);
        bookRepository.delete(book);
        log.info("Deleted book id={}", id);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.BOOKS_CACHE, allEntries = true)
    public Book patchAvailability(Long id, boolean available) {
        Book book = findById(id);
        book.setAvailable(available);
        return bookRepository.save(book);
    }
}
