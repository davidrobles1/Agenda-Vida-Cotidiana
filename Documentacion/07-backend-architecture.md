# 07 — Arquitectura Backend

## Decisión
**Monolito modular** para V1–V3 inicialmente.

### Justificación
- un solo despliegue;
- menor coste operativo;
- transacciones simples;
- debugging más sencillo;
- menor superficie de red;
- límites de dominio explícitos;
- permite extraer módulos posteriormente.

Microservicios se reconsiderarán solo si aparecen necesidades concretas: escalado independiente, equipos independientes, límites operativos claros o cargas muy diferentes.

## Stack
- Java 21 LTS.
- Spring Boot 3.x.
- Spring Security.
- Spring Data JPA/Hibernate.
- PostgreSQL.
- Flyway.
- Bean Validation.
- OpenAPI.
- Testcontainers.

## Estructura
```text
backend/
  src/main/java/com/vidacotidiana/
    shared/
    identity/
    user/
    reminder/
    sharing/
    notification/
    audit/
```

**DECISION (ADR-006):** se añade el módulo `sharing` para modelar invitaciones y colaboradores sobre un recordatorio (owner + colaboradores 1:N, `INVITATION.status`: `PENDING/ACCEPTED/REJECTED/EXPIRED/CANCELLED` — sin `REVOKED`, DEC-003). `reminder` sigue siendo dueño del agregado Reminder; `sharing` no accede directamente a las tablas internas de `reminder`, solo a través de sus interfaces de dominio/application. La revocación de acceso ya aceptado (`REMINDER_SHARE.status = ACTIVE/REVOKED`) es un concepto distinto que vive en `REMINDER_SHARE`, nunca en `INVITATION` (ver `09-data-model.md`).

**DECISION (ADR-007, actualizado por DEC-010):** `notification` se amplía para incluir un puerto `PushNotificationSender` (interfaz propia) con un adapter concreto para **Firebase Cloud Messaging (FCM)**, usado como proveedor unificado para Android, iOS (puente FCM↔APNs) y Web (Web Push). Las notificaciones locales siguen resolviéndose en el cliente (Android/iOS/Web), no en el backend.

**DECISION (DEC-005):** `notification` (o `user`, a definir en la fase de implementación) gestiona la entidad `DEVICE_PUSH_TOKEN`, con soporte para múltiples dispositivos activos por usuario.

**DECISION (DEC-009, reabierta el 2026-08-15 — ver ADR-014 en `22-decision-log.md`):** `sharing` expone un puerto/interfaz de envío de correo (`EmailSender`, mismo patrón que `PushNotificationSender` en `notification`, ADR-007) con proveedor concreto **`TBD`** — dependía de `DEC-008` (AWS → Amazon SES), que fue revertida (V1 usa un servidor propio alquilado, no AWS). La implementación actual usa un adapter **no-op/log-only** (registra en log que se "enviaría" un email, sin enviar nada real) hasta que `DEC-009` se resuelva; el proveedor real se conecta detrás de la misma interfaz sin cambiar el resto del módulo.

**DECISION (DEC-015):** `user` implementa el flujo de soft delete (`deletion_status`, `purge_at`) y un job periódico de purga; `sharing` implementa el job de purga de invitaciones resueltas sin cuenta asociada.

Cada módulo:
```text
api/
application/
domain/
infrastructure/
```

## Reglas
- domain no depende de infrastructure;
- API no contiene reglas de negocio;
- application orquesta casos de uso;
- repositories son interfaces del dominio/application;
- infraestructura implementa adapters;
- módulos no acceden directamente a repositorios internos de otro módulo.

## Concurrencia (RECOMMENDATION técnica, no es una decisión de negocio)
- **`reminder`:** `REMINDER` incorpora un campo `version` (bloqueo optimista) para evitar actualizaciones perdidas cuando el propietario edita un recordatorio o cuando el propietario y un colaborador activo alternan el estado de completado casi simultáneamente (ver `09-data-model.md`). `PATCH /reminders/{id}` y `POST /reminders/{id}/complete` deben validar `version` y responder `409` si no coincide con la esperada por el cliente.
- **`sharing`:** la transición `INVITATION.status = PENDING → ACCEPTED/REJECTED` debe implementarse como una actualización condicional atómica en base de datos (`UPDATE ... WHERE status = 'PENDING'`) para que, ante dos solicitudes concurrentes sobre la misma invitación, solo una tenga éxito y la otra reciba `410` (invitación ya resuelta) en vez de crear dos `REMINDER_SHARE` para la misma invitación.

## Observabilidad — healthcheck
El backend expone un endpoint de healthcheck operativo (Spring Boot Actuator `/actuator/health`) para orquestación/monitoring (NFR-006). Este endpoint es no autenticado, de solo lectura, y vive **fuera** del contrato versionado `/api/v1` definido en `openapi/openapi.yaml` — no forma parte de la superficie de negocio de la API y por lo tanto no requiere autorización OWNER/COLLABORATOR ni aparece en el OpenAPI de negocio.

**DECISION, implementada y verificada con evidencia real (INFRA-006, 2026-08-16, ver `01-technical-backlog.md`):**
- Solo `health` está expuesto (`management.endpoints.web.exposure.include=health`) — ningún otro endpoint de Actuator (`env`, `beans`, `mappings`, etc.) se expone, para no ampliar innecesariamente la superficie de ataque en V1.
- `management.endpoint.health.show-details=when-authorized`: un caller sin bearer token solo ve `{"status":"UP"|"DOWN"}`; el desglose por componente (`db`, `diskSpace`, `ping`) solo se muestra a un caller autenticado.
- El chequeo de Postgres viene del `DataSourceHealthIndicator` de Spring Boot (gratis con `spring-boot-starter-data-jpa` + driver de Postgres) — confirmado real apagando y encendiendo el contenedor de Postgres, no asumido.
- `SecurityConfig` tiene una excepción explícita (`permitAll()`) para `/actuator/health` en la cadena de filtros del resource server OAuth2 — sin ella, todo path exige bearer token por defecto.
