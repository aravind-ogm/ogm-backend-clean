package com.ogm.market.repository;

import com.ogm.market.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("""
        SELECT p FROM Property p
        WHERE
            (:q IS NULL OR LOWER(p.title) LIKE %:q%
                OR LOWER(p.location) LIKE %:q%
                OR LOWER(p.description) LIKE %:q%
            )
        AND (:type IS NULL OR LOWER(p.type) = LOWER(:type))
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:minSqft IS NULL OR p.carpetArea >= :minSqft)
        AND (:maxSqft IS NULL OR p.carpetArea <= :maxSqft)
        AND (:rera IS NULL OR p.reraApproved = :rera)
        AND (:bhk IS NULL OR p.bedrooms = :bhk)
        AND (:facing IS NULL OR LOWER(p.facing) = LOWER(:facing))
        AND (:furnishing IS NULL OR LOWER(p.furnishing) = LOWER(:furnishing))
        """)
    Page<Property> advancedSearch(
            @Param("q") String q,
            @Param("type") String type,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("minSqft") Integer minSqft,
            @Param("maxSqft") Integer maxSqft,
            @Param("rera") Boolean rera,
            @Param("bhk") Integer bhk,
            @Param("facing") String facing,
            @Param("furnishing") String furnishing,
            Pageable pageable
    );
}

