package com.vidacotidiana.routine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Maps to the ROUTINE entity in Documentacion/09-data-model.md (ADR-016
 * adenda Fase 3e2, FR-032). Owner-only, same shape as person.domain.Person.
 *
 * Deliberately has no {@code completed} field (FR-032): the same routine is
 * completed over and over, so its permanent state is {@code active} and the
 * current occurrence is expressed by advancing {@code nextExecutionDate}.
 *
 * A Routine never generates a Reminder or a Commitment — an explicit Product
 * Owner decision (FR-032) that keeps this increment separate from 3d
 * (Automatizaciones simples). There is no scheduler, job or trigger behind
 * this entity.
 */
@Entity
@Table(name = "routines")
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutineFrequency frequency;

    @Column(name = "next_execution_date", nullable = false)
    private Instant nextExecutionDate;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Routine() {
        // JPA
    }

    public Routine(UUID ownerUserId, String title, String description, RoutineFrequency frequency,
                   Instant nextExecutionDate) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = description;
        this.frequency = frequency;
        this.nextExecutionDate = nextExecutionDate;
        this.active = true;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RoutineFrequency getFrequency() {
        return frequency;
    }

    public Instant getNextExecutionDate() {
        return nextExecutionDate;
    }

    public boolean isActive() {
        return active;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }

    /**
     * Marks the current occurrence as done and advances
     * {@code nextExecutionDate} to the next one (UC-25, AC-019).
     *
     * <p><b>DECISION (Product Owner, 2026-08-28 — opción B):</b> the advance
     * is computed from the <b>originally scheduled date</b>, never from
     * "now". A weekly routine scheduled for Monday and marked done on
     * Thursday moves to the following <b>Monday</b>, not the following
     * Thursday — the cadence survives a late execution instead of drifting
     * forward with every delay.
     *
     * <p>Consequence of that rule, stated rather than smoothed over: a
     * routine left unmarked for several periods stays in the past after one
     * click, because one click marks <b>one</b> occurrence. It is not
     * fast-forwarded past today — that would silently discard the missed
     * occurrences, which nobody decided.
     *
     * <p>Month-end is delegated to {@code java.time}: 31 Jan + 1 month is 28
     * (or 29) Feb, not an invalid date. Computed in UTC, consistent with the
     * rest of the codebase storing Instants.
     */
    public void markExecuted() {
        ZonedDateTime scheduled = this.nextExecutionDate.atZone(ZoneOffset.UTC);
        ZonedDateTime advanced = switch (this.frequency) {
            case DAILY -> scheduled.plusDays(1);
            case WEEKLY -> scheduled.plusWeeks(1);
            case MONTHLY -> scheduled.plusMonths(1);
        };
        this.nextExecutionDate = advanced.toInstant();
        this.updatedAt = Instant.now();
    }

    /** Partial update — a null argument leaves the corresponding field unchanged, same pattern as Person#applyEdit. */
    public void applyEdit(String title, String description, RoutineFrequency frequency,
                          Instant nextExecutionDate, Boolean active) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (frequency != null) {
            this.frequency = frequency;
        }
        if (nextExecutionDate != null) {
            this.nextExecutionDate = nextExecutionDate;
        }
        if (active != null) {
            this.active = active;
        }
        this.updatedAt = Instant.now();
    }
}
