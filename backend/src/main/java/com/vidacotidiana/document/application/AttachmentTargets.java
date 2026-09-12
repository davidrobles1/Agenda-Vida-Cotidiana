package com.vidacotidiana.document.application;

import com.vidacotidiana.inventory.domain.InventoryItemRepository;
import com.vidacotidiana.maintenance.domain.MaintenanceRecordRepository;
import com.vidacotidiana.reminder.domain.ReminderRepository;
import com.vidacotidiana.subscription.domain.SubscriptionRepository;
import com.vidacotidiana.warranty.domain.WarrantyRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * DE QUÉ SE PUEDE COLGAR UN DOCUMENTO, Y CÓMO SE COMPRUEBA QUE ES TUYO.
 *
 * EL PROBLEMA QUE RESUELVE. `DocumentService.link` validaba que el DOCUMENTO
 * fuera del llamante y no validaba el DESTINO en absoluto: ni que existiera, ni
 * que perteneciera al mismo usuario, ni que el tipo fuera uno de los
 * soportados. Un `resourceId` cualquiera —incluido el de otra persona— se
 * aceptaba y se guardaba. Que la aplicación Android solo ofrezca destinos
 * válidos no es una defensa: la API es la frontera, y quien llama puede no ser
 * la aplicación.
 *
 * POR QUÉ UN REGISTRO Y NO UNA CADENA DE `if`. Los tipos admitidos son un dato,
 * no una rama de código. Aquí cada tipo es una entrada del mapa que dice cómo
 * se pregunta «¿existe y es suyo?» a su propio módulo; añadir un módulo que
 * acepte adjuntos es añadir una línea, y olvidarse de validarlo es imposible
 * porque el que no está en el mapa se rechaza. La comprobación de propiedad la
 * hace cada repositorio con su propia consulta, que es donde vive esa verdad.
 */
@Component
public class AttachmentTargets {

    /** El tipo, tal como viaja en la API y como se guarda en `documents.resource_type`. */
    private final Map<String, BiPredicate<UUID, UUID>> byType;

    public AttachmentTargets(
            ReminderRepository reminders,
            WarrantyRepository warranties,
            MaintenanceRecordRepository maintenance,
            InventoryItemRepository inventory,
            SubscriptionRepository subscriptions) {
        this.byType = Map.of(
                "REMINDER", reminders::existsByIdAndOwnerUserId,
                "WARRANTY", warranties::existsByIdAndOwnerUserId,
                "MAINTENANCE", maintenance::existsByIdAndOwnerUserId,
                "INVENTORY", inventory::existsByIdAndOwnerUserId,
                "SUBSCRIPTION", subscriptions::existsByIdAndOwnerUserId);
    }

    /** Los tipos admitidos, para poder decirlos en el mensaje de error. */
    public Set<String> supportedTypes() {
        return byType.keySet();
    }

    public boolean isSupported(String resourceType) {
        return resourceType != null && byType.containsKey(normalize(resourceType));
    }

    /**
     * ¿Existe ese destino y es de este usuario?
     *
     * Responde `false` tanto si no existe como si es de otra persona, a
     * propósito: distinguir los dos casos revelaría que el recurso existe, que
     * es justo la enumeración que el resto de la API evita (AC-004/SEC-001).
     */
    public boolean belongsTo(String resourceType, UUID resourceId, UUID ownerUserId) {
        if (resourceId == null) {
            return false;
        }
        BiPredicate<UUID, UUID> check = byType.get(normalize(resourceType));
        return check != null && check.test(resourceId, ownerUserId);
    }

    private static String normalize(String resourceType) {
        return resourceType == null ? null : resourceType.trim().toUpperCase();
    }
}
