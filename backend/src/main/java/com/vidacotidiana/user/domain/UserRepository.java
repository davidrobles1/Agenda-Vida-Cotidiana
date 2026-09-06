package com.vidacotidiana.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // BE-017: resolves an invitation recipient by email (may or may not have an account,
    // SEC-001 — the API response must never reveal which) or by username (sharing.application.SharingService).
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    /**
     * ADR-025 §1 — búsqueda de personas por nombre de usuario para invitarlas
     * a la familia.
     *
     * Coincidencia por PREFIJO (`term%`), no por contiene (`%term%`): el
     * índice único uq_users_username la resuelve con un recorrido de índice,
     * mientras que un comodín inicial obliga a leer la tabla entera — que es
     * exactamente el coste que el mínimo de 5 caracteres busca evitar.
     * `LOWER` a los dos lados para que la búsqueda no distinga mayúsculas.
     *
     * El tope de resultados lo impone quien llama (FamilyService): buscar
     * personas no es paginar un listado propio, y devolver el padrón entero
     * sería el problema que este método evita.
     */
    @Query(value = "SELECT * FROM users WHERE LOWER(username) LIKE LOWER(:term) || '%' "
            + "ORDER BY username LIMIT :limit", nativeQuery = true)
    List<User> searchByUsername(@Param("term") String term, @Param("limit") int limit);

    // BE-027: accounts whose 30-day grace period has elapsed, due for the purge job.
    List<User> findByDeletionStatusAndPurgeAtBefore(String deletionStatus, Instant cutoff);
}
