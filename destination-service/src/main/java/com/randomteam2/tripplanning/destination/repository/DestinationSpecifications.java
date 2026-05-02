package com.randomteam2.tripplanning.destination.repository;

import com.randomteam2.tripplanning.destination.model.Destination;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic {@link Specification} for S2-F1 destination search (category and/or rating bounds).
 */
public final class DestinationSpecifications {

    private DestinationSpecifications() {
    }

    public static Specification<Destination> withOptionalFilters(
            Destination.Category category,
            Double minRating,
            Double maxRating) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }
            if (maxRating != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), maxRating));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
