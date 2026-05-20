package com.example.library.repository;

import com.example.library.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByEmail(String email);

    boolean existsByEmail(String email);

    // Custom JPQL query - search by name (case-insensitive)
    @Query("SELECT a FROM Author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Author> searchByName(@Param("name") String name, Pageable pageable);

    // Custom query - find by nationality
    Page<Author> findByNationalityIgnoreCase(String nationality, Pageable pageable);

    // Fetch authors with their books eagerly (avoids N+1)
    @Query("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id")
    Optional<Author> findByIdWithBooks(@Param("id") Long id);

    // Count books per author
    @Query("SELECT a FROM Author a WHERE SIZE(a.books) >= :minBooks")
    List<Author> findAuthorsWithAtLeastNBooks(@Param("minBooks") int minBooks);

    // Native query example
    @Query(value = "SELECT * FROM authors WHERE nationality IS NOT NULL ORDER BY name",
           nativeQuery = true)
    List<Author> findAllWithNationality();
}
