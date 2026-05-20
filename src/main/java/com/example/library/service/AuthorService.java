package com.example.library.service;

import com.example.library.config.CacheConfig;
import com.example.library.dto.AuthorDTO;
import com.example.library.entity.Author;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthorService {

    private final AuthorRepository authorRepository;

    @Cacheable(value = CacheConfig.AUTHORS_CACHE, key = "#id")
    public Author findById(Long id) {
        return authorRepository.findByIdWithBooks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
    }

    public Page<Author> findAll(Pageable pageable) {
        return authorRepository.findAll(pageable);
    }

    public Page<Author> searchByName(String name, Pageable pageable) {
        return authorRepository.searchByName(name, pageable);
    }

    public Page<Author> findByNationality(String nationality, Pageable pageable) {
        return authorRepository.findByNationalityIgnoreCase(nationality, pageable);
    }

    public List<Author> findAuthorsWithAtLeastNBooks(int minBooks) {
        return authorRepository.findAuthorsWithAtLeastNBooks(minBooks);
    }

    @Transactional
    @CachePut(value = CacheConfig.AUTHORS_CACHE, key = "#result.id")
    public Author create(AuthorDTO.Request request) {
        if (request.getEmail() != null && authorRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Author with email '" + request.getEmail() + "' already exists");
        }
        Author author = Author.builder()
                .name(request.getName())
                .email(request.getEmail())
                .biography(request.getBiography())
                .nationality(request.getNationality())
                .build();
        Author saved = authorRepository.save(author);
        log.info("Created author id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    @CachePut(value = CacheConfig.AUTHORS_CACHE, key = "#id")
    public Author update(Long id, AuthorDTO.Request request) {
        Author author = findById(id);

        if (request.getEmail() != null &&
                !request.getEmail().equals(author.getEmail()) &&
                authorRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email '" + request.getEmail() + "' is already in use");
        }

        author.setName(request.getName());
        author.setEmail(request.getEmail());
        author.setBiography(request.getBiography());
        author.setNationality(request.getNationality());

        Author saved = authorRepository.save(author);
        log.info("Updated author id={}", id);
        return saved;
    }

    @Transactional
    @CacheEvict(value = CacheConfig.AUTHORS_CACHE, key = "#id")
    public void delete(Long id) {
        Author author = findById(id);
        if (!author.getBooks().isEmpty()) {
            throw new BusinessException("Cannot delete author with existing books. Remove the books first.");
        }
        authorRepository.delete(author);
        log.info("Deleted author id={}", id);
    }

    public AuthorDTO.Summary toSummary(Author author) {
        return AuthorDTO.Summary.builder()
                .id(author.getId())
                .name(author.getName())
                .nationality(author.getNationality())
                .build();
    }
}
