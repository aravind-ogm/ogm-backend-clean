package com.ogm.market.repository;

import com.ogm.market.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findBySlug(String slug);

    // ═══════════════════════════════════════════════════════════════
    //  EXISTING METHODS — unchanged
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT DISTINCT *
        FROM properties p
        WHERE
            (:q IS NULL OR
                p.title       ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.location    ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.description ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
            )
        AND (:type       IS NULL OR p.type       ILIKE CONCAT('%', CAST(:type       AS TEXT), '%'))
        AND (:minPrice   IS NULL OR p.price      >= CAST(:minPrice   AS DOUBLE PRECISION))
        AND (:maxPrice   IS NULL OR p.price      <= CAST(:maxPrice   AS DOUBLE PRECISION))
        AND (:rera       IS NULL OR p.rera_approved = CAST(:rera      AS BOOLEAN))
        AND (:bhk        IS NULL OR p.bedrooms   = CAST(:bhk          AS INTEGER))
        AND (:facing     IS NULL OR p.facing     ILIKE CAST(:facing    AS TEXT))
        AND (:furnishing IS NULL OR p.furnishing ILIKE CAST(:furnishing AS TEXT))
        """,
            countQuery = """
        SELECT count(DISTINCT p.id)
        FROM properties p
        WHERE
            (:q IS NULL OR
                p.title       ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.location    ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.description ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
            )
        AND (:type       IS NULL OR p.type       ILIKE CONCAT('%', CAST(:type       AS TEXT), '%'))
        AND (:minPrice   IS NULL OR p.price      >= CAST(:minPrice   AS DOUBLE PRECISION))
        AND (:maxPrice   IS NULL OR p.price      <= CAST(:maxPrice   AS DOUBLE PRECISION))
        AND (:rera       IS NULL OR p.rera_approved = CAST(:rera      AS BOOLEAN))
        AND (:bhk        IS NULL OR p.bedrooms   = CAST(:bhk          AS INTEGER))
        AND (:facing     IS NULL OR p.facing     ILIKE CAST(:facing    AS TEXT))
        AND (:furnishing IS NULL OR p.furnishing ILIKE CAST(:furnishing AS TEXT))
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

    List<Property> findByLocationContainingIgnoreCaseAndPriceLessThanEqual(
            String location, Double price
    );

    @Query(value = """
        SELECT *
        FROM properties
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> semanticSearch(
            @Param("embedding") String embedding,
            @Param("limit")     int limit
    );

    @Query(value = """
        SELECT *
        FROM properties
        WHERE
            (:location IS NULL OR location ILIKE CONCAT('%', CAST(:location AS TEXT), '%'))
        AND (:bhk      IS NULL OR bedrooms = CAST(:bhk     AS INTEGER))
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

    @Query(value = """
        SELECT DISTINCT p.*
        FROM properties p
        JOIN property_amenities pa ON p.id = pa.property_id
        WHERE pa.amenity ILIKE CONCAT('%', CAST(:amenity AS TEXT), '%')
        AND (:maxPrice IS NULL OR p.price <= CAST(:maxPrice AS DOUBLE PRECISION))
        AND (:type     IS NULL OR p.type  ILIKE CONCAT('%', CAST(:type AS TEXT), '%'))
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> searchByAmenity(
            @Param("amenity")   String amenity,
            @Param("maxPrice")  Double maxPrice,
            @Param("type")      String type,
            @Param("limit")     int limit
    );

    @Query(value = """
        SELECT DISTINCT p.*
        FROM properties p
        LEFT JOIN property_amenities pa ON p.id = pa.property_id
        WHERE
            (
                p.title       ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
                p.location    ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
                p.description ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
                p.type        ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
                p.facing      ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
                p.furnishing  ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%') OR
                pa.amenity    ILIKE CONCAT('%', CAST(:keyword AS TEXT), '%')
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
    //  NEW — extended structured search (covers all 19 scenarios)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Extended search used by the AI agent.
     * All new fields (developer, sqft, vastu, possession, listing type) are
     * added here while the existing advancedSearch signature is preserved.
     *
     * Scenarios covered: 1,2,5,6,7,8,9,10,11,12,16,17,18,19
     */
    @Query(value = """
        SELECT DISTINCT p.*
        FROM properties p
        WHERE
            (:q IS NULL OR
                p.title          ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.location       ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.description    ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.developer_name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
            )
        AND (:type        IS NULL OR p.type        ILIKE CONCAT('%', CAST(:type        AS TEXT), '%'))
        AND (:minPrice    IS NULL OR p.price       >= CAST(:minPrice    AS DOUBLE PRECISION))
        AND (:maxPrice    IS NULL OR p.price       <= CAST(:maxPrice    AS DOUBLE PRECISION))
        AND (:rera        IS NULL OR p.rera_approved = CAST(:rera       AS BOOLEAN))
        AND (:bhk         IS NULL OR p.bedrooms    = CAST(:bhk          AS INTEGER))
        AND (:facing      IS NULL OR p.facing      ILIKE CAST(:facing   AS TEXT))
        AND (:furnishing  IS NULL OR p.furnishing  ILIKE CAST(:furnishing AS TEXT))
        AND (:minSqft     IS NULL OR p.sqft        >= CAST(:minSqft     AS INTEGER))
        AND (:maxSqft     IS NULL OR p.sqft        <= CAST(:maxSqft     AS INTEGER))
        AND (:developer   IS NULL OR p.developer_name ILIKE CONCAT('%', CAST(:developer AS TEXT), '%') OR p.title ILIKE CONCAT('%', CAST(:developer AS TEXT), '%'))
        AND (:vastu       IS NULL OR p.vastu_compliant = CAST(:vastu    AS BOOLEAN))
        AND (:possession  IS NULL OR p.possession_status ILIKE CAST(:possession AS TEXT))
        AND (:possessionBefore IS NULL OR p.possession_date <= CAST(:possessionBefore AS DATE))
        AND (:listingType IS NULL OR p.listing_type ILIKE CAST(:listingType AS TEXT))
        AND (:noResale    IS NULL OR :noResale = FALSE OR p.resale = FALSE)
        ORDER BY p.id DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT p.id)
        FROM properties p
        WHERE
            (:q IS NULL OR
                p.title          ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.location       ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.description    ILIKE CONCAT('%', CAST(:q AS TEXT), '%') OR
                p.developer_name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
            )
        AND (:type        IS NULL OR p.type        ILIKE CONCAT('%', CAST(:type        AS TEXT), '%'))
        AND (:minPrice    IS NULL OR p.price       >= CAST(:minPrice    AS DOUBLE PRECISION))
        AND (:maxPrice    IS NULL OR p.price       <= CAST(:maxPrice    AS DOUBLE PRECISION))
        AND (:rera        IS NULL OR p.rera_approved = CAST(:rera       AS BOOLEAN))
        AND (:bhk         IS NULL OR p.bedrooms    = CAST(:bhk          AS INTEGER))
        AND (:facing      IS NULL OR p.facing      ILIKE CAST(:facing   AS TEXT))
        AND (:furnishing  IS NULL OR p.furnishing  ILIKE CAST(:furnishing AS TEXT))
        AND (:minSqft     IS NULL OR p.sqft        >= CAST(:minSqft     AS INTEGER))
        AND (:maxSqft     IS NULL OR p.sqft        <= CAST(:maxSqft     AS INTEGER))
        AND (:developer   IS NULL OR p.developer_name ILIKE CONCAT('%', CAST(:developer AS TEXT), '%') OR p.title ILIKE CONCAT('%', CAST(:developer AS TEXT), '%'))
        AND (:vastu       IS NULL OR p.vastu_compliant = CAST(:vastu    AS BOOLEAN))
        AND (:possession  IS NULL OR p.possession_status ILIKE CAST(:possession AS TEXT))
        AND (:possessionBefore IS NULL OR p.possession_date <= CAST(:possessionBefore AS DATE))
        AND (:listingType IS NULL OR p.listing_type ILIKE CAST(:listingType AS TEXT))
        AND (:noResale    IS NULL OR :noResale = FALSE OR p.resale = FALSE)
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
    //  NEW — radius / distance search using Haversine formula
    //  Cases 3 & 4: "within 10 km from Indiranagar / my location"
    //  No PostGIS required — works with standard PostgreSQL.
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
            WHERE latitude  IS NOT NULL
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
    //  NEW — resolve lat/lng for a named location
    //  Used by distance search to geocode reference locations
    //  (e.g. "within 10 km from Indiranagar") without an external API.
    // ═══════════════════════════════════════════════════════════════

    @Query(value = """
        SELECT latitude, longitude
        FROM properties
        WHERE location ILIKE CONCAT('%', CAST(:location AS TEXT), '%')
        AND latitude  IS NOT NULL
        AND longitude IS NOT NULL
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> findCoordsByLocation(@Param("location") String location);
}