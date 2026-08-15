package com.vidacotidiana.audit.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    // No read endpoint in V1 (not in openapi.yaml) — used only by tests to verify writes.
    List<AuditEvent> findByTargetTypeAndTargetId(AuditTargetType targetType, UUID targetId);
}
