package com.ogm.market.repository;

import com.ogm.market.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    // ── Slug lookup — used by property detail page ─────────────────────────
    // Active check included so hidden properties return 404 on detail page too.
    Optional<Property> findBySlugAndActiveTrue(String slug);

    // Keep original for admin use (admin can view hidden properties by ID)
    Optional<Property> findBySlug(String slug);

    // ═══════════════════════════════════════════════════════════════
    //  STANDARD SEARCH — /api/properties public endpoint
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT DISTINCT p.*
        FROM properties p
        WHERE p.is_active = TRUE
        AND (
            :q IS NULL OR
            p.title       ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.location    ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.description ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
        )
        AND (:type       IS NULL OR p.type        ILIKE CONCAT('%', CAST(:type       AS TEXT), '%'))
        AND (:minPrice   IS NULL OR p.price       >= CAST(:minPrice   AS DOUBLE PRECISION))
        AND (:maxPrice   IS NULL OR p.price       <= CAST(:maxPrice   AS DOUBLE PRECISION))
        AND (:rera       IS NULL OR p.rera_approved = CAST(:rera      AS BOOLEAN))
        AND (:bhk        IS NULL OR p.bedrooms    =  CAST(:bhk        AS INTEGER)
                                OR p.bhk          =  CAST(:bhk        AS TEXT))
        AND (:facing     IS NULL OR p.facing      ILIKE CAST(:facing     AS TEXT))
        AND (:furnishing IS NULL OR p.furnishing  ILIKE CAST(:furnishing AS TEXT))
        ORDER BY p.id DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT p.id)
        FROM properties p
        WHERE p.is_active = TRUE
        AND (
            :q IS NULL OR
            p.title       ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.location    ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.description ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
        )
        AND (:type       IS NULL OR p.type        ILIKE CONCAT('%', CAST(:type       AS TEXT), '%'))
        AND (:minPrice   IS NULL OR p.price       >= CAST(:minPrice   AS DOUBLE PRECISION))
        AND (:maxPrice   IS NULL OR p.price       <= CAST(:maxPrice   AS DOUBLE PRECISION))
        AND (:rera       IS NULL OR p.rera_approved = CAST(:rera      AS BOOLEAN))
        AND (:bhk        IS NULL OR p.bedrooms    =  CAST(:bhk        AS INTEGER)
                                OR p.bhk          =  CAST(:bhk        AS TEXT))
        AND (:facing     IS NULL OR p.facing      ILIKE CAST(:facing     AS TEXT))
        AND (:furnishing IS NULL OR p.furnishing  ILIKE CAST(:furnishing AS TEXT))
        """,
            nativeQuery = true)
    Page<Property> advancedSearch(
            @Param("q")          String q,
            @Param("type")       String type,
            @Param("minPrice")   Double minPrice,
            @Param("maxPrice")   Double maxPrice,
            @Param("rera")       Boolean rera,
            @Param("bhk")        Integer bhk,
            @Param("facing")     String facing,
            @Param("furnishing") String furnishing,
            Pageable pageable
    );

    // ═══════════════════════════════════════════════════════════════
    //  AI EXTENDED SEARCH — all 19 filter scenarios
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT DISTINCT p.*
        FROM properties p
        WHERE p.is_active = TRUE
        AND (
            :q IS NULL OR
            p.title          ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.location       ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.description    ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.developer_name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
        )
        AND (:type        IS NULL OR p.type ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
        AND (:minPrice    IS NULL OR p.price >= CAST(:minPrice AS DOUBLE PRECISION))
        AND (:maxPrice    IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
        AND (:rera        IS NULL OR p.rera_approved = CAST(:rera AS BOOLEAN))
        AND (
             :bhk IS NULL
             OR p.bedrooms = CAST(:bhk AS INTEGER)
             OR p.bhk      = CAST(:bhk AS TEXT)
        )
        AND (:facing      IS NULL OR p.facing     ILIKE CAST(:facing     AS TEXT))
        AND (:furnishing  IS NULL OR p.furnishing ILIKE CAST(:furnishing AS TEXT))
        AND (:minSqft     IS NULL OR p.sqft >= CAST(:minSqft AS INTEGER))
        AND (:maxSqft     IS NULL OR p.sqft <= CAST(:maxSqft AS INTEGER))
        AND (:developer   IS NULL
             OR p.developer_name ILIKE CONCAT('%', CAST(:developer AS TEXT), '%')
             OR p.title          ILIKE CONCAT('%', CAST(:developer AS TEXT), '%')
        )
        AND (:vastu       IS NULL OR p.vastu_compliant = CAST(:vastu AS BOOLEAN))
        AND (:possession  IS NULL OR p.possession_status ILIKE CAST(:possession AS TEXT))
        AND (:possessionBefore IS NULL OR p.possession_date <= CAST(:possessionBefore AS DATE))
        AND (
            :listingType IS NULL
            OR (
                CASE
                    WHEN LOWER(CAST(:listingType AS TEXT)) = 'owner'
                        THEN p.listing_type ILIKE 'owner'
                    ELSE p.listing_type IS NULL
                      OR p.listing_type ILIKE CONCAT('%', CAST(:listingType AS TEXT), '%')
                END
            )
        )
        AND (
            :noResale IS NULL
            OR :noResale = FALSE
            OR p.resale IS NULL
            OR p.resale = FALSE
        )
        ORDER BY p.id DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT p.id)
        FROM properties p
        WHERE p.is_active = TRUE
        AND (
            :q IS NULL OR
            p.title          ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.location       ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.description    ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
            p.developer_name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
        )
        AND (:type        IS NULL OR p.type ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
        AND (:minPrice    IS NULL OR p.price >= CAST(:minPrice AS DOUBLE PRECISION))
        AND (:maxPrice    IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
        AND (:rera        IS NULL OR p.rera_approved = CAST(:rera AS BOOLEAN))
        AND (
             :bhk IS NULL
             OR p.bedrooms = CAST(:bhk AS INTEGER)
             OR p.bhk      = CAST(:bhk AS TEXT)
        )
        AND (:facing      IS NULL OR p.facing     ILIKE CAST(:facing     AS TEXT))
        AND (:furnishing  IS NULL OR p.furnishing ILIKE CAST(:furnishing AS TEXT))
        AND (:minSqft     IS NULL OR p.sqft >= CAST(:minSqft AS INTEGER))
        AND (:maxSqft     IS NULL OR p.sqft <= CAST(:maxSqft AS INTEGER))
        AND (:developer   IS NULL
             OR p.developer_name ILIKE CONCAT('%', CAST(:developer AS TEXT), '%')
             OR p.title          ILIKE CONCAT('%', CAST(:developer AS TEXT), '%')
        )
        AND (:vastu       IS NULL OR p.vastu_compliant = CAST(:vastu AS BOOLEAN))
        AND (:possession  IS NULL OR p.possession_status ILIKE CAST(:possession AS TEXT))
        AND (:possessionBefore IS NULL OR p.possession_date <= CAST(:possessionBefore AS DATE))
        AND (
            :listingType IS NULL
            OR (
                CASE
                    WHEN LOWER(CAST(:listingType AS TEXT)) = 'owner'
                        THEN p.listing_type ILIKE 'owner'
                    ELSE p.listing_type IS NULL
                      OR p.listing_type ILIKE CONCAT('%', CAST(:listingType AS TEXT), '%')
                END
            )
        )
        AND (
            :noResale IS NULL
            OR :noResale = FALSE
            OR p.resale IS NULL
            OR p.resale = FALSE
        )
        """,
            nativeQuery = true)
    Page<Property> extendedSearch(
            @Param("q")                String q,
            @Param("type")             String type,
            @Param("minPrice")         Double minPrice,
            @Param("maxPrice")         Double maxPrice,
            @Param("rera")             Boolean rera,
            @Param("bhk")              Integer bhk,
            @Param("facing")           String facing,
            @Param("furnishing")       String furnishing,
            @Param("minSqft")          Integer minSqft,
            @Param("maxSqft")          Integer maxSqft,
            @Param("developer")        String developer,
            @Param("vastu")            Boolean vastu,
            @Param("possession")       String possession,
            @Param("possessionBefore") LocalDate possessionBefore,
            @Param("listingType")      String listingType,
            @Param("noResale")         Boolean noResale,
            Pageable pageable
    );

    // ═══════════════════════════════════════════════════════════════
    //  RADIUS / DISTANCE SEARCH
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT sub.*
        FROM (
            SELECT *,
                (6371.0 * acos(LEAST(1.0, GREATEST(-1.0,
                    cos(radians(:lat)) * cos(radians(latitude))
                    * cos(radians(longitude) - radians(:lng))
                    + sin(radians(:lat)) * sin(radians(latitude))
                )))) AS dist_km
            FROM properties
            WHERE is_active = TRUE
            AND   latitude  IS NOT NULL
            AND   longitude IS NOT NULL
        ) sub
        WHERE sub.dist_km <= :radiusKm
        AND (:type     IS NULL OR sub.type  ILIKE CONCAT('%', CAST(:type     AS TEXT), '%'))
        AND (:maxPrice IS NULL OR sub.price <= CAST(:maxPrice AS DOUBLE PRECISION))
        ORDER BY sub.dist_km
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> findWithinRadius(
            @Param("lat")       double lat,
            @Param("lng")       double lng,
            @Param("radiusKm")  double radiusKm,
            @Param("type")      String type,
            @Param("maxPrice")  Double maxPrice,
            @Param("limit")     int limit
    );

    // ═══════════════════════════════════════════════════════════════
    //  COORDINATE RESOLUTION
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT latitude, longitude
        FROM properties
        WHERE is_active = TRUE
        AND   location  ILIKE CONCAT('%', CAST(:location AS TEXT), '%')
        AND   latitude  IS NOT NULL
        AND   longitude IS NOT NULL
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> findCoordsByLocation(@Param("location") String location);

    // ═══════════════════════════════════════════════════════════════
    //  AMENITY SEARCH
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT DISTINCT p.*
        FROM properties p
        JOIN property_amenities pa ON p.id = pa.property_id
        WHERE p.is_active = TRUE
        AND   pa.amenity  ILIKE CONCAT('%', CAST(:amenity AS TEXT), '%')
        AND (:maxPrice IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
        AND (:type     IS NULL OR p.type  ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> searchByAmenity(
            @Param("amenity")  String amenity,
            @Param("maxPrice") Double maxPrice,
            @Param("type")     String type,
            @Param("limit")    int limit
    );

    // ═══════════════════════════════════════════════════════════════
    //  DEEP FULL-TEXT KEYWORD SEARCH
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT DISTINCT p.*
        FROM properties p
        LEFT JOIN property_amenities pa ON p.id = pa.property_id
        WHERE p.is_active = TRUE
        AND (
            p.title          ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.location       ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.description    ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.type           ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.facing         ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.furnishing     ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            p.developer_name ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
            pa.amenity       ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%')
        )
        AND (:maxPrice IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
        AND (:type     IS NULL OR p.type  ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> deepSearch(
            @Param("keyword")  String keyword,
            @Param("maxPrice") Double maxPrice,
            @Param("type")     String type,
            @Param("limit")    int limit
    );

    // ═══════════════════════════════════════════════════════════════
    //  SEMANTIC SEARCH — pgvector cosine similarity
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT *
        FROM properties
        WHERE is_active   = TRUE
        AND   embedding   IS NOT NULL
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> semanticSearch(
            @Param("embedding") String embedding,
            @Param("limit")     int limit
    );

    // ═══════════════════════════════════════════════════════════════
    //  HYBRID SEARCH — vector + structured filters
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT *
        FROM properties
        WHERE is_active = TRUE
        AND   embedding IS NOT NULL
        AND (:location IS NULL OR location ILIKE CONCAT('%', CAST(:location AS TEXT), '%'))
        AND (:bhk      IS NULL OR bedrooms = CAST(:bhk AS INTEGER)
                               OR bhk      = CAST(:bhk AS TEXT))
        AND (:maxPrice IS NULL OR price    <= CAST(:maxPrice AS DOUBLE PRECISION))
        AND (:type     IS NULL OR type     ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> hybridSearch(
            @Param("embedding") String embedding,
            @Param("location")  String location,
            @Param("bhk")       Integer bhk,
            @Param("maxPrice")  Double maxPrice,
            @Param("type")      String type,
            @Param("limit")     int limit
    );

    // ═══════════════════════════════════════════════════════════════
    //  EMBEDDING MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE properties
        SET embedding = CAST(:embedding AS vector)
        WHERE id = :id
        """, nativeQuery = true)
    void updateEmbedding(
            @Param("id")        Long id,
            @Param("embedding") String embedding
    );

    @Query(value = """
        SELECT id FROM properties
        WHERE is_active = TRUE
        AND   embedding IS NULL
        ORDER BY id
        """, nativeQuery = true)
    List<Long> findIdsWithNoEmbedding();

    // ═══════════════════════════════════════════════════════════════
    //  CONVENIENCE FINDERS
    // ═══════════════════════════════════════════════════════════════

    List<Property> findByActiveTrueAndSoldOutFalseOrderByIdDesc();

    List<Property> findByActiveTrueAndReraApprovedTrueAndSoldOutFalseOrderByPriceAsc();

    List<Property> findByLocationContainingIgnoreCaseAndPriceLessThanEqualAndActiveTrue(
            String location, Double price
    );
}