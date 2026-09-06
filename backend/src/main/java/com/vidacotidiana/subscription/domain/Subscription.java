package com.vidacotidiana.subscription.domain;

import com.vidacotidiana.shared.domain.ModuleContext;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Módulo Suscripciones (pedido explícito del usuario, 2026-08-22) — mismo
 * shape/patrón que warranty.domain.Warranty (dueño, bloqueo optimista,
 * applyEdit parcial). Campos: service/company/plan/nextPaymentDate/
 * billingCycle — ver V15__subscriptions.sql para el porqué de cada uno
 * (en particular, por qué no hay campo de precio).
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String service;

    @Column
    private String company;

    @Column
    private String plan;

    @Column(name = "next_payment_date", nullable = false)
    private Instant nextPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    /**
     * ADR-019: módulo propietario. Se fija al crear y NO cambia durante el
     * ciclo de vida del recurso — `applyEdit` no lo toca a propósito.
     */
    // ---------------------------------------------------------------
    // ADR-020: compromisos de pago. Todo lo de aquí abajo es aditivo y
    // opcional — una suscripción creada antes de la migración V26 sigue
    // funcionando exactamente igual, con `kind = SUBSCRIPTION` y sin
    // importe.
    // ---------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentKind kind = PaymentKind.SUBSCRIPTION;

    /** Importe del pago. Nullable a propósito: el alta no se bloquea por no
        saberlo todavía, y una tarjeta de crédito no lo conoce hasta el
        corte. Sin importe, el pago no suma al total del periodo. */
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    /** ISO 4217, por pago y no por cuenta (ADR-020(c)). */
    @Column(length = 3)
    private String currency;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column
    private String notes;

    /** Importe cambiante (tarjeta, luz, agua): se muestra "—" en vez de un
        cero inventado. */
    @Column(name = "variable_amount", nullable = false)
    private boolean variableAmount = false;

    // --- Solo kind = CARD ---

    /** Día del mes (1-31) en que corta la tarjeta. */
    @Column(name = "statement_day")
    private Integer statementDay;

    /** Día del mes (1-31) límite de pago. `nextPaymentDate` guarda la
        próxima fecha límite concreta; esto es la regla que la genera. */
    @Column(name = "due_day")
    private Integer dueDay;

    @Column(name = "min_payment", precision = 12, scale = 2)
    private BigDecimal minPayment;

    @Column(name = "no_interest_payment", precision = 12, scale = 2)
    private BigDecimal noInterestPayment;

    @Column
    private String institution;

    /** Etiqueta para reconocer la tarjeta. NO es un dato bancario: nunca se
        pide el número completo, ni CVV, ni titular. */
    @Column(name = "last_four", length = 4)
    private String lastFour;

    // --- Solo kind = CREDIT ---

    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Column(name = "current_installment")
    private Integer currentInstallment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuleContext context = ModuleContext.PERSONAL;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Subscription() {
        // JPA
    }

    public Subscription(UUID ownerUserId, String service, String company, String plan, Instant nextPaymentDate, BillingCycle billingCycle) {
        this(ownerUserId, service, company, plan, nextPaymentDate, billingCycle, ModuleContext.PERSONAL);
    }

    /** ADR-019: alta con módulo propietario explícito. */
    public Subscription(UUID ownerUserId, String service, String company, String plan, Instant nextPaymentDate,
                        BillingCycle billingCycle, ModuleContext context) {
        this.context = (context != null) ? context : ModuleContext.PERSONAL;
        this.ownerUserId = ownerUserId;
        this.service = service;
        this.company = company;
        this.plan = plan;
        this.nextPaymentDate = nextPaymentDate;
        this.billingCycle = billingCycle;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public ModuleContext getContext() {
        return context;
    }

    public PaymentKind getKind() {
        return kind;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isVariableAmount() {
        return variableAmount;
    }

    public Integer getStatementDay() {
        return statementDay;
    }

    public Integer getDueDay() {
        return dueDay;
    }

    public BigDecimal getMinPayment() {
        return minPayment;
    }

    public BigDecimal getNoInterestPayment() {
        return noInterestPayment;
    }

    public String getInstitution() {
        return institution;
    }

    public String getLastFour() {
        return lastFour;
    }

    public Integer getTotalInstallments() {
        return totalInstallments;
    }

    public Integer getCurrentInstallment() {
        return currentInstallment;
    }

    public String getService() {
        return service;
    }

    public String getCompany() {
        return company;
    }

    public String getPlan() {
        return plan;
    }

    public Instant getNextPaymentDate() {
        return nextPaymentDate;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }

    /**
     * ADR-020: aplica los campos de "Pagos". Separado de `applyEdit` porque
     * son opcionales y de otra naturaleza — así el CRUD que ya existía no
     * cambia de firma y sigue funcionando sin enterarse.
     *
     * `null` en cualquier campo = no tocarlo, misma regla que `applyEdit`.
     */
    public void applyPaymentDetails(PaymentDetails details) {
        if (details == null) {
            return;
        }
        if (details.kind() != null) {
            this.kind = details.kind();
        }
        if (details.amount() != null) {
            this.amount = details.amount();
        }
        if (details.currency() != null) {
            this.currency = details.currency();
        }
        if (details.paymentMethod() != null) {
            this.paymentMethod = details.paymentMethod();
        }
        if (details.notes() != null) {
            this.notes = details.notes();
        }
        if (details.variableAmount() != null) {
            this.variableAmount = details.variableAmount();
            // INCOHERENCIA CORREGIDA (2026-08-29): marcar un pago como de
            // importe variable dejaba el importe anterior guardado. La
            // pantalla lo ocultaba, así que quedaba un dato fantasma que
            // reaparecería al desmarcar la casilla — y que además seguía
            // contando en cualquier consulta directa. Si el importe es
            // desconocido, el campo tiene que estar vacío de verdad.
            if (Boolean.TRUE.equals(details.variableAmount())) {
                this.amount = null;
            }
        }
        if (details.statementDay() != null) {
            this.statementDay = details.statementDay();
        }
        if (details.dueDay() != null) {
            this.dueDay = details.dueDay();
        }
        if (details.minPayment() != null) {
            this.minPayment = details.minPayment();
        }
        if (details.noInterestPayment() != null) {
            this.noInterestPayment = details.noInterestPayment();
        }
        if (details.institution() != null) {
            this.institution = details.institution();
        }
        if (details.lastFour() != null) {
            this.lastFour = details.lastFour();
        }
        if (details.totalInstallments() != null) {
            this.totalInstallments = details.totalInstallments();
        }
        if (details.currentInstallment() != null) {
            this.currentInstallment = details.currentInstallment();
        }
        this.updatedAt = Instant.now();
    }

    public void applyEdit(String service, String company, String plan, Instant nextPaymentDate, BillingCycle billingCycle) {
        if (service != null) {
            this.service = service;
        }
        if (company != null) {
            this.company = company;
        }
        if (plan != null) {
            this.plan = plan;
        }
        if (nextPaymentDate != null) {
            this.nextPaymentDate = nextPaymentDate;
        }
        if (billingCycle != null) {
            this.billingCycle = billingCycle;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * ADR-020: avanza al siguiente ciclo tras marcar un pago como
     * realizado. Es lo que impide que la lista mienta al día siguiente.
     *
     * Se calcula sobre la fecha PROGRAMADA, no sobre "hoy": pagar tarde no
     * debe correr todos los vencimientos futuros. Mismo criterio que ya usa
     * `executeRoutine` en el módulo de Rutinas.
     */
    public void advanceToNextCycle() {
        java.time.ZonedDateTime current = this.nextPaymentDate.atZone(java.time.ZoneOffset.UTC);
        java.time.ZonedDateTime next = switch (this.billingCycle) {
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case YEARLY -> current.plusYears(1);
        };
        this.nextPaymentDate = next.toInstant();

        // INCOHERENCIA CORREGIDA (2026-08-29): el contador subía sin tope,
        // así que un crédito de 48 plazos seguía pidiendo el 49, el 50...
        // La aplicación pedía pagos de un crédito ya liquidado, y esos
        // pagos generaban alertas en el calendario y sumaban al total del
        // mes. Ahora se detiene: pasado el último plazo, la fecha deja de
        // avanzar y el compromiso queda terminado.
        if (this.currentInstallment != null) {
            this.currentInstallment = this.currentInstallment + 1;
            if (this.totalInstallments != null && this.currentInstallment > this.totalInstallments) {
                // Se conserva la última fecha en vez de proyectar una nueva:
                // no hay siguiente pago que anunciar.
                this.nextPaymentDate = current.toInstant();
            }
        }
        this.updatedAt = Instant.now();
    }

    /** ADR-020: vuelve al ciclo indicado tras deshacer un pago. */
    public void rewindToPeriod(java.time.LocalDate periodDate) {
        this.nextPaymentDate = periodDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        if (this.currentInstallment != null && this.currentInstallment > 0) {
            this.currentInstallment = this.currentInstallment - 1;
        }
        this.updatedAt = Instant.now();
    }
}
