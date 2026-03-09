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

    Optional<Property> findBySlug(String slug);

    // ================= ADVANCED SEARCH =================
    @Query(value = """
        SELECT DISTINCT *
        FROM properties p
        WHERE
            (:q IS NULL OR
                p.title ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.location ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.description ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
            )
        AND (:type IS NULL OR p.type ILIKE CAST(:type AS TEXT))
        AND (:minPrice IS NULL OR p.price >= CAST(:minPrice AS DOUBLE PRECISION))
        AND (:maxPrice IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
        AND (:rera IS NULL OR p.rera_approved = CAST(:rera AS BOOLEAN))
        AND (:bhk IS NULL OR p.bedrooms = CAST(:bhk AS INTEGER))
        AND (:facing IS NULL OR p.facing ILIKE CAST(:facing AS TEXT))
        AND (:furnishing IS NULL OR p.furnishing ILIKE CAST(:furnishing AS TEXT))
        """,
            countQuery = """
        SELECT count(DISTINCT p.id)
        FROM properties p
        WHERE
            (:q IS NULL OR
                p.title ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.location ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.description ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
            )
        AND (:type IS NULL OR p.type ILIKE CAST(:type AS TEXT))
        AND (:minPrice IS NULL OR p.price >= CAST(:minPrice AS DOUBLE PRECISION))
        AND (:maxPrice IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
        AND (:rera IS NULL OR p.rera_approved = CAST(:rera AS BOOLEAN))
        AND (:bhk IS NULL OR p.bedrooms = CAST(:bhk AS INTEGER))
        AND (:facing IS NULL OR p.facing ILIKE CAST(:facing AS TEXT))
        AND (:furnishing IS NULL OR p.furnishing ILIKE CAST(:furnishing AS TEXT))
        """,
            nativeQuery = true)
    Page<Property> advancedSearch(
            @Param("q") String q,
            @Param("type") String type,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("rera") Boolean rera,
            @Param("bhk") Integer bhk,
            @Param("facing") String facing,
            @Param("furnishing") String furnishing,
            Pageable pageable
    );


    // ================= SIMPLE SEARCH =================
    List<Property> findByLocationContainingIgnoreCaseAndPriceLessThanEqual(
            String location,
            Double price
    );


    // ================= VECTOR SEARCH =================
    @Query(value = """
    SELECT *
    FROM properties
    ORDER BY embedding <-> CAST(:embedding AS vector)
    LIMIT :limit
    """, nativeQuery = true)
    List<Property> semanticSearch(
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );


    // ================= HYBRID AI SEARCH =================
    @Query(value = """
    SELECT *
    FROM properties
    WHERE
        (:location IS NULL OR location ILIKE CONCAT('%', CAST(:location AS TEXT), '%'))
    AND (:bhk IS NULL OR bedrooms = CAST(:bhk AS INTEGER))
    AND (:maxPrice IS NULL OR price <= CAST(:maxPrice AS DOUBLE PRECISION))
    ORDER BY embedding <-> CAST(:embedding AS vector)
    LIMIT :limit
    """, nativeQuery = true)
    List<Property> hybridSearch(
            @Param("embedding") String embedding,
            @Param("location") String location,
            @Param("bhk") Integer bhk,
            @Param("maxPrice") Double maxPrice,
            @Param("limit") int limit
    );

}