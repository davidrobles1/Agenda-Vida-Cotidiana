package com.vidacotidiana.notification.application;

import com.vidacotidiana.maintenance.domain.MaintenanceRecord;
import com.vidacotidiana.maintenance.domain.MaintenanceRecordRepository;
import com.vidacotidiana.notification.domain.PushDueLog;
import com.vidacotidiana.notification.domain.PushDueLogRepository;
import com.vidacotidiana.subscription.domain.Subscription;
import com.vidacotidiana.subscription.domain.SubscriptionRepository;
import com.vidacotidiana.warranty.domain.Warranty;
import com.vidacotidiana.warranty.domain.WarrantyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Los avisos de vencimiento, por push.
 *
 * EL HUECO QUE CIERRA: los vencimientos de pagos, garantías y mantenimientos
 * solo se veían CON LA APLICACIÓN ABIERTA — los calculaba el cliente al
 * cargar cada pantalla. Un pago que vencía con el teléfono en el bolsillo no
 * avisaba de nada, y el push del backend solo cubría familia y compartición.
 *
 * CÓMO EVITA REPETIRSE: antes de mandar nada deja constancia en
 * `push_due_log`, cuya clave primaria es «un aviso por recurso y día» (ver la
 * migración V40). Un reinicio, un despliegue o dos instancias a la vez no
 * duplican el aviso: la segunda choca contra la clave y no manda.
 *
 * DOS MOMENTOS, NO UNO: se avisa tres días antes —para que dé tiempo a hacer
 * algo— y el mismo día. Un único aviso llega tarde o se olvida.
 *
 * SE PUEDE APAGAR (`vidacotidiana.due-notifications.enabled=false`). Un
 * entorno de desarrollo con datos de prueba no tiene por qué mandar avisos.
 */
@Component
public class DueDateNotifier {

    private static final Logger log = LoggerFactory.getLogger(DueDateNotifier.class);

    /** Cuántos días antes se avisa, además del propio día. */
    private static final int DAYS_AHEAD = 3;

    private static final String TYPE_PAYMENT = "PAYMENT";
    private static final String TYPE_WARRANTY = "WARRANTY";
    private static final String TYPE_MAINTENANCE = "MAINTENANCE";

    private final SubscriptionRepository subscriptions;
    private final WarrantyRepository warranties;
    private final MaintenanceRecordRepository maintenances;
    private final PushDueLogRepository dueLog;
    private final PushNotificationSender push;
    private final boolean enabled;
    private final ZoneId zone;

    public DueDateNotifier(
            SubscriptionRepository subscriptions,
            WarrantyRepository warranties,
            MaintenanceRecordRepository maintenances,
            PushDueLogRepository pushDueLog,
            PushNotificationSender push,
            @Value("${vidacotidiana.due-notifications.enabled:true}") boolean enabled,
            @Value("${vidacotidiana.due-notifications.zone:Europe/Madrid}") String zoneId) {
        this.subscriptions = subscriptions;
        this.warranties = warranties;
        this.maintenances = maintenances;
        this.dueLog = pushDueLog;
        this.push = push;
        this.enabled = enabled;
        this.zone = ZoneId.of(zoneId);
    }

    /**
     * Una vez al día, por la mañana.
     *
     * A las 8:00 y no a medianoche a propósito: un aviso que llega mientras
     * alguien duerme se lee —si se lee— con el resto del ruido de la mañana,
     * y ya no distingue lo que reclama de lo que no.
     */
    @Scheduled(cron = "${vidacotidiana.due-notifications.cron:0 0 8 * * *}", zone = "${vidacotidiana.due-notifications.zone:Europe/Madrid}")
    @Transactional
    public void notifyDueSoon() {
        if (!enabled) {
            return;
        }
        LocalDate today = LocalDate.now(zone);
        int sent = 0;
        for (int offset : new int[] {DAYS_AHEAD, 0}) {
            LocalDate target = today.plusDays(offset);
            Instant from = target.atStartOfDay(zone).toInstant();
            Instant to = from.plus(1, ChronoUnit.DAYS);

            for (Subscription s : subscriptions.findByNextPaymentDateBetween(from, to)) {
                sent += notifyOnce(s.getOwnerUserId(), TYPE_PAYMENT, s.getId(), today,
                        PushEventType.PAYMENT_DUE, phrase("Pago de " + s.getService(), offset));
            }
            for (Warranty w : warranties.findByExpiresAtBetween(from, to)) {
                sent += notifyOnce(w.getOwnerUserId(), TYPE_WARRANTY, w.getId(), today,
                        PushEventType.WARRANTY_EXPIRING, phrase("Garantía de " + w.getItem(), offset));
            }
            for (MaintenanceRecord m : maintenances.findByNextDueAtBetween(from, to)) {
                sent += notifyOnce(m.getOwnerUserId(), TYPE_MAINTENANCE, m.getId(), today,
                        PushEventType.MAINTENANCE_DUE, phrase("Mantenimiento de " + m.getItem(), offset));
            }
        }
        if (sent > 0) {
            log.info("Avisos de vencimiento enviados: {}", sent);
        }
    }

    /**
     * Manda el aviso SOLO si no se mandó ya hoy.
     *
     * La constancia se guarda ANTES de enviar: si el envío falla, no se
     * reintenta hoy. Es deliberado — el push ya es best-effort (ver
     * `PushNotificationSender`), y repetir un aviso molesta más que perderlo.
     */
    private int notifyOnce(UUID userId, String type, UUID resourceId, LocalDate today,
                           PushEventType event, String message) {
        if (userId == null || resourceId == null) {
            return 0;
        }
        if (dueLog.existsByUserIdAndResourceTypeAndResourceIdAndNotifiedOn(userId, type, resourceId, today)) {
            return 0;
        }
        dueLog.save(new PushDueLog(userId, type, resourceId, today));
        push.sendBestEffort(userId, new PushEvent(event, message));
        return 1;
    }

    private String phrase(String what, int daysAhead) {
        return daysAhead == 0 ? what + ": vence hoy." : what + ": vence en " + daysAhead + " días.";
    }

    /** Lo de hace más de un mes ya no evita ningún duplicado. */
    @Scheduled(cron = "0 30 3 * * SUN", zone = "${vidacotidiana.due-notifications.zone:Europe/Madrid}")
    @Transactional
    public void prune() {
        int removed = dueLog.deleteOlderThan(LocalDate.now(zone).minusMonths(1));
        if (removed > 0) {
            log.info("Registro de avisos limpiado: {} filas", removed);
        }
    }
}
