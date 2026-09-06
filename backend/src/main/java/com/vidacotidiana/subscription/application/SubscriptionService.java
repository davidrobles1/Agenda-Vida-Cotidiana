package com.vidacotidiana.subscription.application;

import com.vidacotidiana.shared.domain.ModuleContext;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.subscription.domain.BillingCycle;
import com.vidacotidiana.subscription.domain.PaymentDetails;
import com.vidacotidiana.subscription.domain.PaymentRecord;
import com.vidacotidiana.subscription.domain.PaymentRecordRepository;
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
    private final PaymentRecordRepository paymentRecordRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PaymentRecordRepository paymentRecordRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Transactional
    public Subscription create(UUID ownerUserId, String service, String company, String plan, Instant nextPaymentDate, BillingCycle billingCycle) {
        return create(ownerUserId, service, company, plan, nextPaymentDate, billingCycle, ModuleContext.PERSONAL);
    }

    /** ADR-019: alta con el módulo desde el que se creó. */
    @Transactional
    public Subscription create(UUID ownerUserId, String service, String company, String plan, Instant nextPaymentDate,
                               BillingCycle billingCycle, ModuleContext context) {
        return create(ownerUserId, service, company, plan, nextPaymentDate, billingCycle, context, null);
    }

    /** ADR-020: alta con los campos de compromiso de pago. `details` nulo =
        una suscripción como las de antes. */
    @Transactional
    public Subscription create(UUID ownerUserId, String service, String company, String plan, Instant nextPaymentDate,
                               BillingCycle billingCycle, ModuleContext context, PaymentDetails details) {
        Subscription subscription = new Subscription(ownerUserId, service, company, plan, nextPaymentDate, billingCycle, context);
        subscription.applyPaymentDetails(details);
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
        return edit(subscriptionId, callerUserId, service, company, plan, nextPaymentDate, billingCycle,
                expectedVersion, null);
    }

    /** ADR-020: edición con los campos de compromiso de pago. */
    @Transactional
    public Subscription edit(UUID subscriptionId, UUID callerUserId, String service, String company, String plan,
                              Instant nextPaymentDate, BillingCycle billingCycle, int expectedVersion,
                              PaymentDetails details) {
        Subscription subscription = getOwnedOrThrow(subscriptionId, callerUserId);

        if (expectedVersion != subscription.getVersion()) {
            throw new ConflictException("SUBSCRIPTION_VERSION_CONFLICT",
                    "Subscription " + subscriptionId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + subscription.getVersion() + ").");
        }

        subscription.applyEdit(service, company, plan, nextPaymentDate, billingCycle);
        subscription.applyPaymentDetails(details);
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

    /**
     * ADR-020(f): marca el ciclo actual como pagado y avanza al siguiente.
     *
     * IDEMPOTENTE: si ese ciclo ya estaba registrado, devuelve el
     * compromiso sin volver a avanzar la fecha. Sin esto, un doble clic o
     * un reintento de red saltarían dos ciclos y el usuario vería su
     * próximo pago un mes más tarde de lo real.
     *
     * El importe se guarda aparte del compromiso porque puede diferir del
     * estimado —una tarjeta paga una cifra distinta cada mes— y pisar el
     * estimado perdería la referencia.
     */
    @Transactional
    public Subscription markAsPaid(UUID subscriptionId, UUID callerUserId, java.math.BigDecimal paidAmount,
                                   String paidCurrency) {
        Subscription subscription = getOwnedOrThrow(subscriptionId, callerUserId);

        java.time.LocalDate periodDate = subscription.getNextPaymentDate()
                .atZone(java.time.ZoneOffset.UTC).toLocalDate();

        if (paymentRecordRepository.findBySubscriptionIdAndPeriodDate(subscriptionId, periodDate).isPresent()) {
            return subscription;
        }

        paymentRecordRepository.save(new PaymentRecord(
                subscriptionId,
                callerUserId,
                periodDate,
                java.time.LocalDate.now(),
                paidAmount != null ? paidAmount : subscription.getAmount(),
                paidCurrency != null ? paidCurrency : subscription.getCurrency()));

        subscription.advanceToNextCycle();
        return subscriptionRepository.save(subscription);
    }

    /**
     * Deshace el último pago registrado de un compromiso y retrocede la
     * fecha. Existe porque marcar por error es trivial y sin esto la única
     * salida sería editar la fecha a mano.
     */
    @Transactional
    public Subscription undoLastPayment(UUID subscriptionId, UUID callerUserId) {
        Subscription subscription = getOwnedOrThrow(subscriptionId, callerUserId);
        java.util.List<PaymentRecord> history =
                paymentRecordRepository.findBySubscriptionIdOrderByPeriodDateDesc(subscriptionId);
        if (history.isEmpty()) {
            return subscription;
        }
        PaymentRecord last = history.get(0);
        paymentRecordRepository.delete(last);
        subscription.rewindToPeriod(last.getPeriodDate());
        return subscriptionRepository.save(subscription);
    }

    /** Historial de un compromiso concreto. Verifica la propiedad primero:
        el id llega de la ruta y no puede confiarse. */
    @Transactional(readOnly = true)
    public java.util.List<PaymentRecord> listPaymentRecordsFor(UUID subscriptionId, UUID callerUserId) {
        getOwnedOrThrow(subscriptionId, callerUserId);
        return paymentRecordRepository.findBySubscriptionIdOrderByPeriodDateDesc(subscriptionId);
    }

    @Transactional(readOnly = true)
    public java.util.List<PaymentRecord> listPaymentRecords(UUID ownerUserId) {
        return paymentRecordRepository.findByOwnerUserId(ownerUserId);
    }
}
