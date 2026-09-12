package com.vidacotidiana.mood.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * El ánimo de UN DÍA. Implementa la especificación aprobada en
 * Documentacion/35-artefacto-maestro-matriz-capacidades.md §4.1.
 *
 * ES DATO DE SALUD, y eso cambia tres cosas respecto a cualquier otra entidad
 * del sistema — ninguna es de interfaz, las tres son estructurales:
 *
 *  1. NO SE COMPARTE. No hay `resource_shares` para esto y no debe haberlo. Si
 *     algún día alguien añade un tipo compartible, esta entidad no entra.
 *  2. SE BORRA EN DURO. `MoodService.deleteAll` elimina filas. Un borrado
 *     suave aquí sería conservar justo lo que el usuario pidió que
 *     desapareciera.
 *  3. NO SE EXPORTA en las salidas de familia.
 *
 * La fecha es `LocalDate` y no `Instant` a propósito: el ánimo pertenece a un
 * día, no a un instante. Guardar la hora permitiría responder «¿a qué hora
 * estabas mal?», que es exactamente la pregunta que el producto no hace.
 */
@Entity
@Table(name = "mood_entry")
public class MoodEntry {

    /** 0 genial · 1 bien · 2 normal · 3 regular · 4 bajo. Escala cerrada. */
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private short value;

    @Column
    private String note;

    /**
     * Las etiquetas del «¿Qué lo hizo así?».
     *
     * `ElementCollection` y no una entidad propia porque una etiqueta no tiene
     * vida fuera de su día: no se consulta por sí sola, no se comparte y no
     * sobrevive al borrado del ánimo. Modelarla como entidad daría un
     * repositorio y un ciclo de vida que nadie necesita.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mood_entry_tag", joinColumns = @JoinColumn(name = "mood_entry_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MoodEntry() {
    }

    public MoodEntry(UUID ownerUserId, LocalDate entryDate, int value, String note, Set<String> tags) {
        this.ownerUserId = ownerUserId;
        this.entryDate = entryDate;
        setValue(value);
        this.note = normalizeNote(note);
        replaceTags(tags);
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Volver a marcar el mismo día SUSTITUYE, no acumula.
     *
     * Un día tiene un ánimo. Guardar cada corrección dejaría un registro de
     * altibajos dentro de la jornada que el producto no pide y que, siendo dato
     * de salud, es justo lo que conviene no tener.
     */
    public void update(int value, String note, Set<String> tags) {
        setValue(value);
        this.note = normalizeNote(note);
        replaceTags(tags);
        this.updatedAt = Instant.now();
    }

    private void setValue(int value) {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException("mood value out of range: " + value);
        }
        this.value = (short) value;
    }

    /** Una nota en blanco es no tener nota, no tener una nota vacía. */
    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void replaceTags(Set<String> incoming) {
        this.tags.clear();
        if (incoming != null) {
            for (String tag : incoming) {
                if (tag != null && !tag.isBlank()) {
                    this.tags.add(tag.trim());
                }
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public int getValue() {
        return value;
    }

    public String getNote() {
        return note;
    }

    public Set<String> getTags() {
        return tags;
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
}
