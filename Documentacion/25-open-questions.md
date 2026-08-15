# 25 — Preguntas abiertas

## Resueltas (2026-08-09) — ver `22-decision-log.md` y `28-v1-decision-pack.md`
- ~~¿Registro mediante email/password, Google, passkeys, o combinación?~~ → DECISION: Keycloak, OIDC self-hosted con passkeys/MFA (DEC-004/ADR-008).
- ~~¿Los recordatorios V1 son exclusivamente personales o se contempla compartir?~~ → DECISION: compartibles desde V1, modelo owner + colaboradores 1:N vía invitación (ADR-006).
- ~~¿La notificación debe ser local o también push desde backend?~~ → DECISION: ambas; push vía FCM unificado (DEC-010/ADR-007).
- ~~Alcance de plataformas (Android vs. +iOS/Web)~~ → DECISION: Android + iOS + Web desde V1 (ADR-005).
- ~~¿Se requiere verificación de email en V1?~~ → DECISION: delegada a Keycloak, sin sistema propio (DEC-014).
- ~~¿Política de retención/eliminación de cuenta para V1?~~ → DECISION: soft delete con 30 días de gracia; purga de emails de invitados sin cuenta (DEC-015/ADR-012).
- ~~¿Qué ocurre si el propietario elimina un recordatorio que tiene colaboradores activos?~~ → DECISION: se elimina en cascada y se notifica a los colaboradores activos (DEC-002).
- ~~Formato del lifecycle de INVITATION / uso de REVOKED~~ → DECISION: enum corregido a `PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED`; revocación solo en `REMINDER_SHARE` (DEC-003).
- ~~¿Proveedor cloud preferido?~~ → DECISION: servidor propio alquilado, self-hosted (DEC-008/ADR-014; corrige la decisión previa de AWS del 2026-08-09, ver `22-decision-log.md`).
- ¿Proveedor de correo transaccional? — Reabierta el 2026-08-15. Había sido marcada como resuelta (`DECISION: Amazon SES, DEC-009`, 2026-08-09), pero esa elección dependía de que el cloud provider fuera AWS; al revertirse esa decisión (`ADR-014`), `DEC-009` quedó sin la premisa de la que dependía y ninguna de sus opciones originales (Amazon SES / especialista tipo Postmark / self-hosted) fue elegida en su lugar. Sigue `TBD`.
- ~~Proveedor OIDC definitivo~~ → DECISION: Keycloak (DEC-004/ADR-008).
- ~~Proveedor(es) de push por plataforma~~ → DECISION: FCM unificado (DEC-010/ADR-007).
- ~~¿Versión mínima de Android/iOS a soportar? ¿Navegadores web soportados?~~ → DECISION: Android API 30, iOS 17, navegadores evergreen últimas 2 versiones mayores (DEC-011/012/013).
- ~~Stack tecnológico definitivo de iOS y Web~~ → DECISION: Swift+SwiftUI (DEC-006/ADR-010) y React+TypeScript SPA (DEC-007/ADR-011).
- ~~Estado de completado de recordatorios compartidos~~ → DECISION: estado único global por recordatorio (DEC-001).
- ~~Modelo de almacenamiento de tokens de dispositivo/push~~ → DECISION: `DEVICE_PUSH_TOKEN`, multi-dispositivo (DEC-005).

## Pendientes — no bloquean el inicio de V1 (ver `29-v1-final-readiness.md` para el detalle de bloqueo)

### Producto/negocio
1. ¿Nombre definitivo de la aplicación/producto?
2. ¿Mercado/país inicial?
3. ¿Quién será el primer grupo de usuarios de validación?
4. ¿Licencia/nombre de repositorio y organización GitHub?
5. ¿Se requiere una cuenta de usuario real para el MVP o se acepta modo local/offline antes de autenticar?
6. ¿Se requiere modo offline en V1?
7. ¿Existe un límite máximo de colaboradores por recordatorio en V1?
8. Formato/reglas exactas del username/handle (unicidad, caracteres permitidos, cambio posterior) — necesario para la invitación por username.
9. ¿Qué ocurre si el propietario edita significativamente un recordatorio compartido (p. ej. cambia la fecha)? ¿Los colaboradores deben ser notificados?
10. ¿Una invitación pendiente por email se vincula automáticamente si esa persona se registra después?
11. ¿Durante `PENDING_DELETION` (DEC-015), el usuario puede cancelar la solicitud de eliminación de cuenta? Comportamiento exacto de reversión.

### Infraestructura/técnicas
12. ¿V1 tendrá backend desplegado en AWS desde el inicio o entorno local/staging primero?
13. Dispositivos soportados más allá de las versiones mínimas de SO (gama, RAM, etc.) — no cubierto por DEC-011/012/013.
14. Patrón de manejo de tokens OIDC en el cliente Web SPA (memoria + renovación silenciosa) — consideración técnica de implementación, no decisión de negocio (ver `08c-web-architecture.md`).
