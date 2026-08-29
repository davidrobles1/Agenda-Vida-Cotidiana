package com.vidacotidiana.daynote.domain;

import com.vidacotidiana.shared.domain.ModuleContext;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Canvas de notas por día (pedido explícito del usuario, 2026-08-22):
 * "Reemplazar la vista actual de notas por un único Canvas, similar al de
 * Visión Board." Mismo shape que visionboard.domain.VisionBoardElement
 * (x/y/width/height/zIndex/data JSONB/version) pero mucho más restringido
 * — solo 2 `type` posibles (ver DayNoteElementType), sin board_id (el
 * agrupador aquí es `noteDate`, no un id de tablero: "las notas
 * pertenecen al día seleccionado").
 *
 * `data` es el mismo shape para BANNER y TEXT (text/bold/italic) — el
 * usuario aclaró explícitamente que el banner también puede llevar texto
 * dentro ("bandera con texto dentro"), así que ambos tipos comparten el
 * mismo contrato de datos; solo el renderizado visual cambia según
 * `type`.
 */
@Entity
@Table(name = "day_note_elements")
public class DayNoteElement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayNoteElementType type;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double width;

    @Column(nullable = false)
    private double height;

    @Column(name = "z_index", nullable = false)
    private int zIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> data;

    /**
     * ADR-019: módulo propietario. Se fija al crear y NO cambia durante el
     * ciclo de vida del recurso — `applyEdit` no lo toca a propósito.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuleContext context = ModuleContext.PERSONAL;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DayNoteElement() {
        // JPA
    }

    public DayNoteElement(UUID ownerUserId, LocalDate noteDate, DayNoteElementType type,
                           double x, double y, double width, double height, int zIndex, Map<String, Object> data) {
        this(ownerUserId, noteDate, type, x, y, width, height, zIndex, data, ModuleContext.PERSONAL);
    }

    /** ADR-019: alta con módulo propietario explícito. */
    public DayNoteElement(UUID ownerUserId, LocalDate noteDate, DayNoteElementType type,
                           double x, double y, double width, double height, int zIndex, Map<String, Object> data,
                           ModuleContext context) {
        this.context = (context != null) ? context : ModuleContext.PERSONAL;
        this.ownerUserId = ownerUserId;
        this.noteDate = noteDate;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.zIndex = zIndex;
        this.data = (data != null) ? data : Map.of();
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

    public ModuleContext getContext() {
        return context;
    }

    public LocalDate getNoteDate() {
        return noteDate;
    }

    public DayNoteElementType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public int getZIndex() {
        return zIndex;
    }

    public Map<String, Object> getData() {
        return data;
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

    /** Rectángulo actual — usado por DayNoteService para el chequeo de
        no-solapamiento contra los demás elementos del mismo owner+día. */
    public boolean overlaps(double otherX, double otherY, double otherWidth, double otherHeight) {
        return x < otherX + otherWidth
                && x + width > otherX
                && y < otherY + otherHeight
                && y + height > otherY;
    }

    /** Reposicionar/redimensionar (drag/resize) — igual patrón que
        VisionBoardElement#applyPosition. */
    public void applyPosition(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.updatedAt = Instant.now();
    }

    public void applyZIndex(int zIndex) {
        this.zIndex = zIndex;
        this.updatedAt = Instant.now();
    }

    /** El contenido (texto/negrita/cursiva) siempre se reemplaza entero —
        mismo motivo que VisionBoardElement#applyEdit's propio `data`: el
        editor siempre manda su estado completo actual, nunca un parche
        parcial. */
    public void applyData(Map<String, Object> data) {
        this.data = (data != null) ? data : Map.of();
        this.updatedAt = Instant.now();
    }
}
