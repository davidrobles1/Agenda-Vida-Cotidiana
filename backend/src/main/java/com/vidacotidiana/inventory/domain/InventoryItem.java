package com.vidacotidiana.inventory.domain;

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
import java.util.UUID;

/**
 * Módulo Inventario (pedido explícito del usuario, 2026-08-22) — mismo
 * shape/patrón que warranty.domain.Warranty (dueño, bloqueo optimista,
 * applyEdit parcial), campos alineados con el mock previo (name, category,
 * location — ver V13__inventory.sql).
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryCategory category;

    @Column
    private String location;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
        // JPA
    }

    public InventoryItem(UUID ownerUserId, String name, InventoryCategory category, String location) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.category = category;
        this.location = location;
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

    public String getName() {
        return name;
    }

    public InventoryCategory getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
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

    public void applyEdit(String name, InventoryCategory category, String location) {
        if (name != null) {
            this.name = name;
        }
        if (category != null) {
            this.category = category;
        }
        if (location != null) {
            this.location = location;
        }
        this.updatedAt = Instant.now();
    }
}
