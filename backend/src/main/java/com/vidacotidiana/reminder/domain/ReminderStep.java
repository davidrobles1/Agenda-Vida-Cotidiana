package com.vidacotidiana.reminder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Un paso dentro de una tarea (V34).
 *
 * DE DÓNDE SALE. El anillo de avance del artefacto —«4 de 5 pasos · 80 %»— se
 * deriva de aquí. Sin esta tabla el anillo no tendría de qué alimentarse y
 * habría que inventarle un número, que es justo lo que no se hace.
 *
 * EL PORCENTAJE NO VIVE EN NINGÚN SITIO. Se calcula al leer, contando `done`
 * sobre el total. Guardarlo daría dos fuentes de verdad para la misma cifra, y
 * la segunda se desincroniza el día que alguien marque un paso por otra vía.
 *
 * NO LLEVA `ownerUserId`. La pertenencia es la de su tarea: duplicarla aquí
 * abriría la puerta a que las dos discrepen. El servicio comprueba el dueño
 * contra el recordatorio antes de tocar nada.
 */
@Entity
@Table(name = "reminder_steps")
public class ReminderStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reminder_id", nullable = false)
    private UUID reminderId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReminderStep() {
    }

    public ReminderStep(UUID reminderId, String title, int position) {
        this.reminderId = reminderId;
        this.title = title;
        this.position = position;
        this.done = false;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Marcar y desmarcar son el mismo gesto: en el artefacto se toca la casilla. */
    public void toggle() {
        this.done = !this.done;
        this.updatedAt = Instant.now();
    }

    public void rename(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
            this.updatedAt = Instant.now();
        }
    }

    public void moveTo(int position) {
        this.position = position;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getReminderId() {
        return reminderId;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDone() {
        return done;
    }

    public int getPosition() {
        return position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
