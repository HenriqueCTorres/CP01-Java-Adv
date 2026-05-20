package com.example.library.repository;

import com.example.library.entity.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BookSpecification {

    private BookSpecification() {}

    public static Specification<Book> withFilters(
            String title,
            String genre,
            String authorName,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"));
            }

            if (genre != null && !genre.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("genre")),
                        genre.toLowerCase()));
            }

            if (authorName != null && !authorName.isBlank()) {
                var authorJoin = root.join("author");
                predicates.add(cb.like(cb.lower(authorJoin.get("name")),
                        "%" + authorName.toLowerCase() + "%"));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (available != null) {
                predicates.add(cb.equal(root.get("available"), available));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
