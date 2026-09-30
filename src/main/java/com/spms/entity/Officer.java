package com.spms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Not explicitly listed in the spec's entity list, but added because the Officer
 * dashboard/queue APIs need to know which procurement center an officer manages.
 * Mirrors the shape of Farmer for consistency.
 */
@Entity
@Table(name = "officers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Officer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "officer_id", nullable = false, unique = true)
    private String officerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_center_id")
    private ProcurementCenter procurementCenter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
