package com.vidacotidiana.inventory.domain;

/** Pedido explícito del usuario (2026-08-22): "según la categoría que
    muestra: Todos, Electrónicos, Hogar, Vehículos" — Todos es un filtro
    del cliente (sin selección), no una categoría real almacenada. */
public enum InventoryCategory {
    ELECTRONICOS,
    HOGAR,
    VEHICULOS
}
