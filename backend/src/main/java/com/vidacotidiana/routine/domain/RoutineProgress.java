package com.vidacotidiana.routine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cuánto se llevó hecho de una rutina en un día (V35).
 *
 * UNA FILA POR RUTINA Y DÍA, garantizado por la restricción única de la tabla.
 * El progreso no se acumula en la propia rutina porque entonces el histórico
 * habría que reconstruirlo, y corregir un día anterior obligaría a recalcular
 * todo lo posterior. Así, cada día es independiente y la racha se cuenta
 * leyendo, no manteniendo un contador que se desincroniza.
 *
 * `LocalDate` y no `Instant`: un hábito es de un día. A qué hora se bebió el
 * cuarto vaso de agua no es una pregunta que el producto haga.
 */
@Entity
@Table(name = "routine_progress")
public class RoutineProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "routine_id", nullable = false)
    private UUID routineId;

    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;

    @Column(nullable = false)
    private int count;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoutineProgress() {
    }

    public RoutineProgress(UUID routineId, LocalDate progressDate, int count) {
        this.routineId = routineId;
        this.progressDate = progressDate;
        this.count = Math.max(0, count);
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Sumar (o restar) sobre lo que ya había.
     *
     * Nunca baja de cero: tocar «menos» en un día sin nada hecho no debe dejar
     * un contador negativo que luego habría que interpretar.
     */
    public void add(int delta) {
        this.count = Math.max(0, this.count + delta);
        this.updatedAt = Instant.now();
    }

    /** Fijar el valor exacto, para corregir un día en vez de ir de uno en uno. */
    public void set(int value) {
        this.count = Math.max(0, value);
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoutineId() {
        return routineId;
    }

    public LocalDate getProgressDate() {
        return progressDate;
    }

    public int getCount() {
        return count;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
