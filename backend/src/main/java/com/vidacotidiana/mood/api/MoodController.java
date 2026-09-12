package com.vidacotidiana.mood.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.mood.api.dto.MoodResponse;
import com.vidacotidiana.mood.api.dto.UpsertMoodRequest;
import com.vidacotidiana.mood.application.MoodService;
import com.vidacotidiana.mood.domain.MoodEntry;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Los cuatro endpoints de §4.1 de
 * Documentacion/35-artefacto-maestro-matriz-capacidades.md, ni uno más.
 *
 * NO HAY ENDPOINT DE COMPARTIR NI DE EXPORTAR, y no debe añadirse: el ánimo es
 * dato de salud y su superficie de acceso es deliberadamente la mínima. Todas
 * las rutas trabajan sobre `currentUser`; ninguna acepta un id de usuario.
 */
@RestController
@RequestMapping("/api/v1/moods")
public class MoodController {

    private final MoodService moodService;
    private final CurrentUser currentUser;

    public MoodController(MoodService moodService, CurrentUser currentUser) {
        this.moodService = moodService;
        this.currentUser = currentUser;
    }

    /**
     * Marcar el ánimo. Devuelve 200 y no 201 a propósito: es un upsert sobre
     * (usuario, día), así que la segunda llamada del mismo día no crea nada.
     */
    @PostMapping
    public MoodResponse upsert(@Valid @RequestBody UpsertMoodRequest request) {
        MoodEntry entry = moodService.upsert(
                currentUser.userId(), request.date(), request.value(), request.note(), request.tags());
        return MoodResponse.from(entry);
    }

    /** El rango que pinta la semana y el mes. Sin parámetros: últimos 30 días. */
    @GetMapping
    public List<MoodResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return moodService.list(currentUser.userId(), from, to).stream()
                .map(MoodResponse::from)
                .toList();
    }

    @GetMapping("/{date}")
    public MoodResponse get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return MoodResponse.from(moodService.get(currentUser.userId(), date));
    }

    /**
     * Borrado DURO de todo el historial (Ajustes → Privacidad).
     *
     * Devuelve cuántos registros cayeron: la interfaz dice «se borraron 21
     * registros», que es una confirmación comprobable, no un «listo».
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAll() {
        int deleted = moodService.deleteAll(currentUser.userId());
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
