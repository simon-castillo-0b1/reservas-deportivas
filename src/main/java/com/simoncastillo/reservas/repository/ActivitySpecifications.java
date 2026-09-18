package com.simoncastillo.reservas.repository;

import com.simoncastillo.reservas.entity.Activity;
import com.simoncastillo.reservas.entity.ActivityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivitySpecifications {

    private ActivitySpecifications() {
        //nadie puede crear una instancia de esta clase
    }

    public static Specification<Activity> withFilters(String sport, ActivityStatus status, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (sport != null && !sport.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("sport")), sport.trim().toLowerCase()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
