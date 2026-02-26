package com.ogm.market.repository;

import com.ogm.market.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    // ================= FIND BY SLUG =================
    Optional<Property> findBySlug(String slug);

    // ================= ADVANCED SEARCH =================
    @Query("""
        SELECT p FROM Property p
        WHERE
            (:q IS NULL OR
                LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.location) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))
            )
        AND (:type IS NULL OR LOWER(p.type) = LOWER(:type))
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:rera IS NULL OR p.reraApproved = :rera)
        AND (:bhk IS NULL OR LOWER(p.bedrooms) LIKE LOWER(CONCAT('%', :bhk, '%')))
        AND (:facing IS NULL OR LOWER(p.facing) = LOWER(:facing))
        AND (:furnishing IS NULL OR LOWER(p.furnishing) = LOWER(:furnishing))
    """)
    Page<Property> advancedSearch(
            @Param("q") String q,
            @Param("type") String type,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("rera") Boolean rera,
            @Param("bhk") String bhk,
            @Param("facing") String facing,
            @Param("furnishing") String furnishing,
            Pageable pageable
    );

    // ================= SIMPLE SEARCH =================
    List<Property> findByLocationContainingIgnoreCaseAndPriceLessThanEqual(
            String location,
            Double price
    );
}