package com.vidacotidiana.sharing.application;

import com.vidacotidiana.document.domain.DocumentRepository;
import com.vidacotidiana.inventory.domain.InventoryItemRepository;
import com.vidacotidiana.maintenance.domain.MaintenanceRecordRepository;
import com.vidacotidiana.reminder.domain.ReminderRepository;
import com.vidacotidiana.sharing.domain.SharedResourceType;
import com.vidacotidiana.subscription.domain.SubscriptionRepository;
import com.vidacotidiana.warranty.domain.WarrantyRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduce (tipo, id) al mínimo que la compartición necesita saber de un
 * recurso: quién es su dueño, cómo se llama y qué fecha lo define.
 *
 * POR QUÉ UNA SOLA CLASE Y NO SEIS ADAPTADORES. Cada módulo podría exponer un
 * puerto propio, pero eso son seis interfaces y seis implementaciones para
 * responder tres preguntas de solo lectura. La regla de no sobrearquitecturar
 * de CLAUDE.md ("¿podemos resolverlo de manera más simple?") apunta a esto:
 * un único punto de traducción, explícito, que se lee entero de una vez y que
 * es trivial de partir en adaptadores el día que algún módulo se separe.
 *
 * NO MUTA NADA. Solo lee repositorios que ya existían; ningún módulo cambia de
 * comportamiento porque exista esta clase.
 */
@Component
public class SharedResourceCatalog {

    private final ReminderRepository reminderRepository;
    private final MaintenanceRecordRepository maintenanceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WarrantyRepository warrantyRepository;
    private final InventoryItemRepository inventoryRepository;
    private final DocumentRepository documentRepository;

    public SharedResourceCatalog(ReminderRepository reminderRepository,
                                  MaintenanceRecordRepository maintenanceRepository,
                                  SubscriptionRepository subscriptionRepository,
                                  WarrantyRepository warrantyRepository,
                                  InventoryItemRepository inventoryRepository,
                                  DocumentRepository documentRepository) {
        this.reminderRepository = reminderRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.warrantyRepository = warrantyRepository;
        this.inventoryRepository = inventoryRepository;
        this.documentRepository = documentRepository;
    }

    public Optional<ResourceRef> find(SharedResourceType type, UUID id) {
        return switch (type) {
            case REMINDER -> reminderRepository.findById(id)
                    .map(r -> new ResourceRef(type, id, r.getOwnerUserId(), r.getTitle(), r.getDueAt()));
            case MAINTENANCE -> maintenanceRepository.findById(id)
                    .map(m -> new ResourceRef(type, id, m.getOwnerUserId(), m.getItem(), m.getNextDueAt()));
            case SUBSCRIPTION -> subscriptionRepository.findById(id)
                    .map(s -> new ResourceRef(type, id, s.getOwnerUserId(), s.getService(), s.getNextPaymentDate()));
            case WARRANTY -> warrantyRepository.findById(id)
                    .map(w -> new ResourceRef(type, id, w.getOwnerUserId(), w.getItem(), w.getExpiresAt()));
            case INVENTORY_ITEM -> inventoryRepository.findById(id)
                    .map(i -> new ResourceRef(type, id, i.getOwnerUserId(), i.getName(), null));
            case DOCUMENT -> documentRepository.findById(id)
                    .map(d -> new ResourceRef(type, id, d.getOwnerUserId(), d.getName(), null));
        };
    }

    /**
     * Lo que la sección Compartidos necesita para pintar una fila sin duplicar
     * el recurso: su nombre tal cual lo escribió el dueño y la fecha que lo
     * define (vencimiento, próximo pago o próxima revisión). {@code date} es
     * nulo donde el recurso no tiene una — un artículo de inventario o un
     * documento no vencen.
     */
    public record ResourceRef(SharedResourceType type, UUID id, UUID ownerUserId, String label, Instant date) {
    }
}
