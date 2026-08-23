package com.vidacotidiana.subscription.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Owner-only, mismo motivo que warranty.domain.WarrantyRepository. */
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Page<Subscription> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
