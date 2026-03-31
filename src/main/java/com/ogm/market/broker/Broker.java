package com.ogm.market.broker;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maps exactly to your Supabase `brokers` table.
 * operating_areas and property_types are stored as JSONB arrays.
 */

@Entity
@Table(name = "brokers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Broker {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "rera_number", length = 100)
    private String reraNumber;

    @Column(name = "mobile_number", nullable = false, unique = true, length = 15)
    private String mobile;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "office_address", nullable = false, columnDefinition = "TEXT")
    private String officeAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operating_areas", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> operatingAreas = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "property_types", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> propertyTypes = new ArrayList<>();

    @Column(name = "number_of_properties")
    private Integer numberOfProperties;

    @Column(name = "total_listings")
    @Builder.Default
    private Integer totalListings = 0;

    @Column(name = "total_leads")
    @Builder.Default
    private Integer totalLeads = 0;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private BrokerStatus status = BrokerStatus.PENDING;

    @Column(name = "auth_provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.EMAIL;

    @Column(name = "google_id", length = 255)
    private String googleId;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "profile_picture_url", length = 255)
    private String profilePictureUrl;

    @Column(name = "agreed_to_terms", nullable = false)
    private boolean agreedToTerms = false;

    @Column(name = "mobile_verified", nullable = false)
    private boolean mobileVerified = false;

    @Column(name = "is_mobile_verified", nullable = false)
    private boolean isMobileVerified = false;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "is_email_verified", nullable = false)
    private boolean isEmailVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    public enum BrokerStatus {
        PENDING, APPROVED, REJECTED, SUSPENDED
    }

    public enum AuthProvider {
        LOCAL, GOOGLE, WHATSAPP, EMAIL
    }

    // IMPORTANT — This fixes your NULL ID problem
    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}