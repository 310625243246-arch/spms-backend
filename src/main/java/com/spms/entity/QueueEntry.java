package com.spms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Named QueueEntry (table "queue_entries") rather than "Queue" to keep the
 * name unambiguous in JPQL/HQL queries. One row per procurement center per day.
 */
@Entity
@Table(name = "queue_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"procurement_center_id", "queue_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_center_id", nullable = false)
    private ProcurementCenter procurementCenter;

    @Column(name = "queue_date", nullable = false)
    private LocalDate date;

    @Column(name = "current_position", nullable = false)
    private Integer currentPosition;

    @Column(name = "total_people", nullable = false)
    private Integer totalPeople;
}
