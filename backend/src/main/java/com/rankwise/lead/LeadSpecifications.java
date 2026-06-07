package com.rankwise.lead;

import org.springframework.data.jpa.domain.Specification;

public final class LeadSpecifications {

    private LeadSpecifications() {
    }

    public static Specification<Lead> withFilters(String search, String category, String gender) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("mobile")), term),
                        cb.like(cb.lower(root.get("category")), term),
                        cb.like(cb.lower(root.get("branch")), term),
                        cb.like(cb.lower(root.get("gender")), term),
                        cb.like(root.get("rank").as(String.class), "%" + search.trim() + "%")
                ));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }
            if (gender != null && !gender.isBlank()) {
                predicates.add(cb.equal(root.get("gender"), gender.trim().toUpperCase()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
