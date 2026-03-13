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
        AND (:type IS NULL OR p.type ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
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
        AND (:type IS NULL OR p.type ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
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
            String location, Double price
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


    // ================= HYBRID AI SEARCH (with type) =================
    @Query(value = """
    SELECT *
    FROM properties
    WHERE
        (:location IS NULL OR location ILIKE CONCAT('%', CAST(:location AS TEXT), '%'))
    AND (:bhk IS NULL OR bedrooms = CAST(:bhk AS INTEGER))
    AND (:maxPrice IS NULL OR price <= CAST(:maxPrice AS DOUBLE PRECISION))
    AND (:type IS NULL OR type ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
    ORDER BY embedding <-> CAST(:embedding AS vector)
    LIMIT :limit
    """, nativeQuery = true)
    List<Property> hybridSearch(
            @Param("embedding") String embedding,
            @Param("location") String location,
            @Param("bhk") Integer bhk,
            @Param("maxPrice") Double maxPrice,
            @Param("type") String type,
            @Param("limit") int limit
    );


    // ================= AMENITY SEARCH =================
    // Search properties that have a specific amenity
    @Query(value = """
    SELECT DISTINCT p.*
    FROM properties p
    JOIN property_amenities pa ON p.id = pa.property_id
    WHERE pa.amenity ILIKE CONCAT('%', CAST(:amenity AS TEXT), '%')
    AND (:maxPrice IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
    AND (:type IS NULL OR p.type ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
    LIMIT :limit
    """, nativeQuery = true)
    List<Property> searchByAmenity(
            @Param("amenity") String amenity,
            @Param("maxPrice") Double maxPrice,
            @Param("type") String type,
            @Param("limit") int limit
    );


    // ================= FULL TEXT DEEP SEARCH =================
    // Searches across title, location, description, AND amenities table
    @Query(value = """
    SELECT DISTINCT p.*
    FROM properties p
    LEFT JOIN property_amenities pa ON p.id = pa.property_id
    WHERE
        (
            p.title ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.location ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.description ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.type ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.facing ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.furnishing ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            pa.amenity ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%')
        )
    AND (:maxPrice IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
    AND (:type IS NULL OR p.type ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
    LIMIT :limit
    """, nativeQuery = true)
    List<Property> deepSearch(
            @Param("keyword") String keyword,
            @Param("maxPrice") Double maxPrice,
            @Param("type") String type,
            @Param("limit") int limit
    );

}