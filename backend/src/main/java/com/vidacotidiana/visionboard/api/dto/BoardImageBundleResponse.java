package com.vidacotidiana.visionboard.api.dto;

import java.util.List;

/**
 * Todas las imágenes de un tablero, en UNA respuesta.
 *
 * POR QUÉ EXISTE: pintar un Vision Board con veinte fotos costaba veinte
 * peticiones. Con una conexión de baja latencia da igual; con una normal
 * —datos móviles, un túnel, cualquier cosa con 300 ms de ida y vuelta— eso
 * son segundos enteros que no dependen del tamaño de las fotos sino de
 * CUÁNTAS veces se pregunta. Bajar una sola vez todo lo que el tablero
 * necesita convierte esos segundos en un viaje.
 *
 * Va en base64 dentro del JSON, y es una decisión consciente: infla los
 * bytes un tercio, pero ahorra diecinueve viajes y no obliga a inventar un
 * formato multiparte propio ni a que el cliente lo desmonte. Con las
 * miniaturas (ver {@code ImageThumbnailer}) el bulto queda en unos cientos
 * de KB, así que el tercio de más cuesta mucho menos que la latencia que
 * evita.
 *
 * Las imágenes que no son del usuario no viajan: el paquete se arma con las
 * suyas y nada más, igual que haría cada GET por separado.
 */
public record BoardImageBundleResponse(List<Item> items) {

    public record Item(String id, String contentType, String dataBase64) {
    }
}
