package com.vidacotidiana.mood.application;

import com.vidacotidiana.mood.domain.MoodEntry;
import com.vidacotidiana.mood.domain.MoodEntryRepository;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * El ánimo del día, según Documentacion/35-artefacto-maestro-matriz-capacidades.md §4.1.
 *
 * TRES REGLAS QUE NO SE NEGOCIAN, por ser dato de salud:
 *
 *  · Todo método recibe `ownerUserId` y lo usa en la consulta. No existe un
 *    `getById` sin dueño, ni siquiera para uso interno: la pertenencia va en
 *    la firma, no en un `if` que alguien pueda olvidar.
 *  · `deleteAll` borra de verdad. No marca, no archiva.
 *  · No hay método de compartir ni de exportar, y no debe añadirse.
 */
@Service
public class MoodService {

    /** Ventana máxima de una consulta de rango: un año. */
    private static final int MAX_RANGE_DAYS = 366;

    private final MoodEntryRepository repository;

    public MoodService(MoodEntryRepository repository) {
        this.repository = repository;
    }

    /**
     * Marcar el ánimo de un día.
     *
     * ES UN UPSERT, y por eso es idempotente sobre (usuario, fecha): tocar otra
     * carita en la misma jornada corrige la de hoy en vez de apilar una
     * segunda. La restricción única de la tabla dice lo mismo a nivel de
     * esquema, así que dos peticiones simultáneas no pueden crear dos filas.
     */
    @Transactional
    public MoodEntry upsert(UUID ownerUserId, LocalDate date, int value, String note, Set<String> tags) {
        LocalDate day = date == null ? LocalDate.now() : date;
        if (day.isAfter(LocalDate.now())) {
            throw new ValidationException("No se puede registrar el ánimo de un día que todavía no ha pasado.");
        }
        if (value < MoodEntry.MIN_VALUE || value > MoodEntry.MAX_VALUE) {
            throw new ValidationException("El ánimo va de " + MoodEntry.MIN_VALUE + " a " + MoodEntry.MAX_VALUE + ".");
        }
        return repository.findByOwnerUserIdAndEntryDate(ownerUserId, day)
                .map(existing -> {
                    existing.update(value, note, tags);
                    return existing;
                })
                .orElseGet(() -> repository.save(new MoodEntry(ownerUserId, day, value, note, tags)));
    }

    @Transactional(readOnly = true)
    public MoodEntry get(UUID ownerUserId, LocalDate date) {
        return repository.findByOwnerUserIdAndEntryDate(ownerUserId, date)
                .orElseThrow(() -> new NotFoundException("MOOD_NOT_FOUND",
                        "No hay ánimo registrado para ese día."));
    }

    /**
     * El rango que pinta la semana y el mes de Bienestar.
     *
     * Se acota a un año: una consulta abierta sobre dato de salud es justo la
     * que no conviene poder hacer de un tirón, ni siquiera siendo el dueño.
     */
    @Transactional(readOnly = true)
    public List<MoodEntry> list(UUID ownerUserId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw new ValidationException("La fecha inicial es posterior a la final.");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new ValidationException("El rango no puede superar " + MAX_RANGE_DAYS + " días.");
        }
        return repository.findByOwnerUserIdAndEntryDateBetweenOrderByEntryDateDesc(ownerUserId, start, end);
    }

    @Transactional(readOnly = true)
    public long count(UUID ownerUserId) {
        return repository.countByOwnerUserId(ownerUserId);
    }

    /**
     * Borrado DURO de todo el historial (Ajustes → Privacidad del artefacto).
     *
     * Devuelve cuántas filas cayeron para que la interfaz pueda decir «se
     * borraron 21 registros» en vez de un «listo» que no prueba nada.
     */
    @Transactional
    public int deleteAll(UUID ownerUserId) {
        return repository.deleteAllByOwnerUserId(ownerUserId);
    }
}
