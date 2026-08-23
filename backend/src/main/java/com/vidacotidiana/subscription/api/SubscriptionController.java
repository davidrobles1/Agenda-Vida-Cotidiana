package com.vidacotidiana.subscription.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.shared.api.PageResponse;
import com.vidacotidiana.subscription.api.dto.CreateSubscriptionRequest;
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
                currentUser.userId(), request.service(), request.company(), request.plan(), request.nextPaymentDate(), request.billingCycle());
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.from(created));
    }

    @GetMapping
    public PageResponse<SubscriptionResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Subscription> subscriptions = subscriptionService.listOwnedBy(currentUser.userId(), pageable);
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
                request.nextPaymentDate(), request.billingCycle(), request.version());
        return SubscriptionResponse.from(subscription);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        subscriptionService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
