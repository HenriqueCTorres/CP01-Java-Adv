package com.example.library.repository;

import com.example.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    // Find by author
    Page<Book> findByAuthorId(Long authorId, Pageable pageable);

    // Find by genre
    Page<Book> findByGenreIgnoreCase(String genre, Pageable pageable);

    // Find available books
    Page<Book> findByAvailableTrue(Pageable pageable);

    // Full-text search on title and description
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> searchByTitleOrDescription(@Param("query") String query, Pageable pageable);

    // Price range filter
    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :min AND :max")
    Page<Book> findByPriceRange(@Param("min") BigDecimal min,
                                 @Param("max") BigDecimal max,
                                 Pageable pageable);

    // Books by author name
    @Query("SELECT b FROM Book b JOIN b.author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%'))")
    Page<Book> findByAuthorName(@Param("authorName") String authorName, Pageable pageable);

    // Bulk update availability
    @Modifying
    @Query("UPDATE Book b SET b.available = :available WHERE b.author.id = :authorId")
    int updateAvailabilityByAuthor(@Param("authorId") Long authorId,
                                    @Param("available") boolean available);

    // Statistics
    @Query("SELECT b.genre, COUNT(b), AVG(b.price) FROM Book b GROUP BY b.genre ORDER BY COUNT(b) DESC")
    List<Object[]> getStatsByGenre();

    // Most expensive books
    @Query("SELECT b FROM Book b WHERE b.price IS NOT NULL ORDER BY b.price DESC")
    List<Book> findTopByPrice(Pageable pageable);
}
