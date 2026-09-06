package com.vidacotidiana.subscription.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.shared.api.PageResponse;
import com.vidacotidiana.shared.domain.ModuleContext;
import com.vidacotidiana.subscription.api.dto.CreateSubscriptionRequest;
import com.vidacotidiana.subscription.api.dto.MarkPaidRequest;
import com.vidacotidiana.subscription.api.dto.PaymentRecordResponse;
import com.vidacotidiana.subscription.api.dto.SubscriptionResponse;
import com.vidacotidiana.subscription.api.dto.UpdateSubscriptionRequest;
import com.vidacotidiana.subscription.application.SubscriptionService;
import com.vidacotidiana.subscription.domain.Subscription;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Módulo Suscripciones (pedido explícito del usuario, 2026-08-22) — mismo
    shape que WarrantyController. */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUser currentUser;

    public SubscriptionController(SubscriptionService subscriptionService, CurrentUser currentUser) {
        this.subscriptionService = subscriptionService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody CreateSubscriptionRequest request) {
        Subscription created = subscriptionService.create(
                currentUser.userId(), request.service(), request.company(), request.plan(), request.nextPaymentDate(),
                request.billingCycle(), ModuleContext.fromNullable(request.context()), request.toPaymentDetails());
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.from(created));
    }

    @GetMapping
    public PageResponse<SubscriptionResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            // ADR-019: sin `context` se devuelve todo (Calendario general);
            // con contexto, el filtro baja hasta la consulta SQL.
            @RequestParam(value = "context", required = false) String context) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Subscription> subscriptions = subscriptionService.listOwnedBy(
                currentUser.userId(), ModuleContext.filterFromNullable(context), pageable);
        return PageResponse.from(subscriptions.map(SubscriptionResponse::from));
    }

    @GetMapping("/{id}")
    public SubscriptionResponse get(@PathVariable UUID id) {
        Subscription subscription = subscriptionService.getOwnedOrThrow(id, currentUser.userId());
        return SubscriptionResponse.from(subscription);
    }

    @PatchMapping("/{id}")
    public SubscriptionResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionService.edit(
                id, currentUser.userId(), request.service(), request.company(), request.plan(),
                request.nextPaymentDate(), request.billingCycle(), request.version(), request.toPaymentDetails());
        return SubscriptionResponse.from(subscription);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        subscriptionService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    /**
     * ADR-020(f): marca el ciclo actual como pagado y avanza al siguiente.
     * Idempotente — ver `SubscriptionService.markAsPaid`.
     */
    @PostMapping("/{id}/payments")
    public SubscriptionResponse markPaid(@PathVariable UUID id,
                                         @Valid @RequestBody(required = false) MarkPaidRequest request) {
        Subscription updated = subscriptionService.markAsPaid(
                id,
                currentUser.userId(),
                request != null ? request.amount() : null,
                request != null ? request.currency() : null);
        return SubscriptionResponse.from(updated);
    }

    /**
     * Historial de un compromiso. Existe porque el detalle debe poder
     * responder "¿ya pagué esto el mes pasado?" — hasta ahora los registros
     * se guardaban y no había forma de verlos.
     */
    @GetMapping("/{id}/payments")
    public java.util.List<PaymentRecordResponse> listPaymentsFor(@PathVariable UUID id) {
        return subscriptionService.listPaymentRecordsFor(id, currentUser.userId()).stream()
                .map(PaymentRecordResponse::from)
                .toList();
    }

    /** Deshace el último pago registrado y retrocede la fecha. */
    @DeleteMapping("/{id}/payments/last")
    public SubscriptionResponse undoLastPayment(@PathVariable UUID id) {
        return SubscriptionResponse.from(subscriptionService.undoLastPayment(id, currentUser.userId()));
    }

    /**
     * Todos los ciclos pagados del usuario. En bloque y no por compromiso:
     * la pantalla necesita saber qué filas van marcadas como pagadas, y
     * pedirlo uno por uno sería una consulta por fila.
     */
    @GetMapping("/payments")
    public java.util.List<PaymentRecordResponse> listPayments() {
        return subscriptionService.listPaymentRecords(currentUser.userId()).stream()
                .map(PaymentRecordResponse::from)
                .toList();
    }
}
