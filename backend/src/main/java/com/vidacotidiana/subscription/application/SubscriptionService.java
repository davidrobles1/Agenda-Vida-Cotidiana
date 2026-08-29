package com.vidacotidiana.subscription.application;

import com.vidacotidiana.shared.domain.ModuleContext;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.subscription.domain.BillingCycle;
import com.vidacotidiana.subscription.domain.Subscription;
import com.vidacotidiana.subscription.domain.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Módulo Suscripciones — mismo patrón exacto que warranty.application.WarrantyService. */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Subscription create(UUID ownerUserId, String service, String company, String plan, Instant nextPaymentDate, BillingCycle billingCycle) {
        return create(ownerUserId, service, company, plan, nextPaymentDate, billingCycle, ModuleContext.PERSONAL);
    }

    /** ADR-019: alta con el módulo desde el que se creó. */
    @Transactional
    public Subscription create(UUID ownerUserId, String service, String company, String plan, Instant nextPaymentDate,
                               BillingCycle billingCycle, ModuleContext context) {
        Subscription subscription = new Subscription(ownerUserId, service, company, plan, nextPaymentDate, billingCycle, context);
        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public Page<Subscription> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return listOwnedBy(ownerUserId, null, pageable);
    }

    /**
     * ADR-019: `context` nulo = sin filtrar (Calendario general). Con
     * contexto, el filtro va en la consulta.
     */
    @Transactional(readOnly = true)
    public Page<Subscription> listOwnedBy(UUID ownerUserId, ModuleContext context, Pageable pageable) {
        return (context == null)
                ? subscriptionRepository.findByOwnerUserId(ownerUserId, pageable)
                : subscriptionRepository.findByOwnerUserIdAndContext(ownerUserId, context, pageable);
    }

    @Transactional(readOnly = true)
    public Subscription getOwnedOrThrow(UUID subscriptionId, UUID callerUserId) {
        Subscription subscription = findOrThrow(subscriptionId);
        requireOwner(subscription, callerUserId);
        return subscription;
    }

    @Transactional
    public Subscription edit(UUID subscriptionId, UUID callerUserId, String service, String company, String plan,
                              Instant nextPaymentDate, BillingCycle billingCycle, int expectedVersion) {
        Subscription subscription = getOwnedOrThrow(subscriptionId, callerUserId);

        if (expectedVersion != subscription.getVersion()) {
            throw new ConflictException("SUBSCRIPTION_VERSION_CONFLICT",
                    "Subscription " + subscriptionId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + subscription.getVersion() + ").");
        }

        subscription.applyEdit(service, company, plan, nextPaymentDate, billingCycle);
        try {
            return subscriptionRepository.save(subscription);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("SUBSCRIPTION_VERSION_CONFLICT",
                    "Subscription " + subscriptionId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID subscriptionId, UUID callerUserId) {
        Subscription subscription = getOwnedOrThrow(subscriptionId, callerUserId);
        subscriptionRepository.delete(subscription);
    }

    private Subscription findOrThrow(UUID subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("SUBSCRIPTION_NOT_FOUND", "The requested subscription was not found."));
    }

    private void requireOwner(Subscription subscription, UUID callerUserId) {
        if (!subscription.isOwnedBy(callerUserId)) {
            throw new NotFoundException("SUBSCRIPTION_NOT_FOUND", "The requested subscription was not found.");
        }
    }
}
