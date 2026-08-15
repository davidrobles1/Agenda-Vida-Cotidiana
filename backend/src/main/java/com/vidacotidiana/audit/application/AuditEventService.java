package com.vidacotidiana.audit.application;

import com.vidacotidiana.audit.domain.AuditEvent;
import com.vidacotidiana.audit.domain.AuditEventRepository;
import com.vidacotidiana.audit.domain.AuditEventType;
import com.vidacotidiana.audit.domain.AuditTargetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * BE-029. Callers use this instead of writing SLF4J-only log lines for the
 * events 11-auth-security.md §Auditoría lists — record() runs inside the
 * caller's own @Transactional method (no propagation override), so a
 * rollback of the business operation also rolls back its audit row: unlike
 * push, an audit event must never be recorded for an operation that failed.
 */
@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(AuditEventType eventType, UUID actorUserId, AuditTargetType targetType, UUID targetId) {
        auditEventRepository.save(new AuditEvent(eventType, actorUserId, targetType, targetId));
    }
}
