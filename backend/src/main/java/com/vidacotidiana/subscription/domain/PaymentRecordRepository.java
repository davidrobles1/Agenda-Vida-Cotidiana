package com.vidacotidiana.subscription.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    /** Historial de un compromiso, lo más reciente primero. */
    List<PaymentRecord> findBySubscriptionIdOrderByPeriodDateDesc(UUID subscriptionId);

    /** ADR-020: todos los pagos registrados del usuario. La pantalla los
        necesita en bloque para saber qué filas van marcadas como pagadas,
        y pedirlos uno por uno sería una consulta por fila. */
    List<PaymentRecord> findByOwnerUserId(UUID ownerUserId);

    /** Sostiene la idempotencia de "marcar como pagado" junto al índice
        único de la migración V26. */
    Optional<PaymentRecord> findBySubscriptionIdAndPeriodDate(UUID subscriptionId, java.time.LocalDate periodDate);
}
