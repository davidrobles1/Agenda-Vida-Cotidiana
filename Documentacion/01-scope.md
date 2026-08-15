# 01 — Alcance del proyecto

## Objetivo
Construir una plataforma (Android, iOS y Web) orientada a organizar información y actividades de la vida cotidiana.

**DECISION (2026-08-09):** V1 se construye para las tres plataformas — Android, iOS y Web — de forma coherente, en lugar de lanzar Android en solitario. Sustituye el supuesto anterior de "solo Android" registrado en el draft inicial. Ver ADR-005.

**Nota histórica (ya resuelta):** en el draft inicial, el stack tecnológico de iOS y Web quedaba pendiente de análisis. Esto fue resuelto por el Product Owner el 2026-08-09: iOS usa Swift + SwiftUI nativo (DEC-006/ADR-010, ver `08b-ios-architecture.md`) y Web usa React + TypeScript como SPA (DEC-007/ADR-011, ver `08c-web-architecture.md`). Ver la sección "Supuestos / decisiones cerradas" más abajo.

## Alcance confirmado
### V1
MVP mínimo para:
- crear una cuenta (Android, iOS, Web);
- iniciar/cerrar sesión;
- gestionar sesión/dispositivo;
- ver Home;
- crear, consultar, actualizar y completar recordatorios/tareas;
- **compartir un recordatorio con una o varias personas mediante invitación (modelo simple owner + colaboradores, ver `DECISION` en ADR-006 y FR-007 a FR-010);**
- recibir notificaciones locales y **notificaciones push desde backend** (FCM) para recordatorios propios y compartidos (ver ADR-007 y FR-011/FR-012);
- base de privacidad, seguridad, auditoría técnica y observabilidad;
- configuración básica de cuenta, incluyendo **solicitar la eliminación de la cuenta** (soft delete, FR-013, consecuencia directa de DEC-015).

### V2
- estabilización;
- hardening;
- mejoras de UX;
- sincronización más robusta;
- recuperación de errores;
- pruebas ampliadas;
- mejoras de observabilidad;
- preparación para crecimiento.

### V3
- funcionalidades adicionales del producto, aún no especificadas;
- rendimiento;
- escalabilidad;
- optimizaciones de almacenamiento y API;
- colaboración/familia si se aprueba.

### V4
Versión final objetivo. El alcance funcional exacto es `TBD`.

### Posterior a V4
- IA: fuera de V1–V4.
- Finanzas: fuera de V1–V4.

## Fuera de alcance inicial
- integración bancaria;
- estados de cuenta;
- IA;
- marketplace;
- afiliados;
- publicidad;
- microservicios;
- arquitectura distribuida compleja;
- archivos/adjuntos en recordatorios;
- grupos, hogares, equipos o roles múltiples entre colaboradores (más allá de owner/colaborador simple);
- búsqueda social o libreta de contactos para compartir.

**DOCUMENTATION_CONFLICT resuelto:** el draft inicial excluía "soporte iOS" y "web app pública" de V1, mientras que CLAUDE.md exige documentar y construir las tres plataformas de forma coherente. Se resolvió a favor de CLAUDE.md — ver DECISION arriba y ADR-005.

## Supuestos / decisiones cerradas (28-v1-decision-pack.md, 2026-08-09)
- Plataformas V1: Android, iOS, Web (DECISION, ADR-005).
- Mercado inicial: TBD.
- Modelo de monetización: TBD para V1.
- Cloud provider: **servidor propio alquilado (self-hosted)** (DECISION, DEC-008/ADR-014). Proveedor de hosting concreto: `TBD`.
- Proveedor de correo: `TBD` — `DEC-009` reabierta (ver nota histórica abajo).

**Nota histórica (corregida el 2026-08-15):** el 2026-08-09 se había aprobado `AWS` como cloud provider (`DEC-008/ADR-009`) y, en consecuencia, `Amazon SES` como proveedor de correo (`DEC-009`). El Product Owner revirtió explícitamente la elección de cloud provider antes de continuar con Milestone 2: V1 usa un servidor propio alquilado, no AWS ni ningún servicio gestionado de AWS (ver `ADR-014` en `22-decision-log.md`). Como `DEC-009` dependía de `DEC-008`, queda reabierta como `TBD` — no se eligió un proveedor de correo alternativo sin instrucción explícita del Product Owner.
- Proveedor de identidad (OIDC self-hosted): **Keycloak** (DECISION, DEC-004/ADR-008).
- Proveedor de push: **Firebase Cloud Messaging (FCM)**, unificado para las tres plataformas (DECISION, DEC-010/ADR-007).
- Stack iOS: **Swift + SwiftUI** (DECISION, DEC-006/ADR-010). Ver `08b-ios-architecture.md`.
- Stack Web: **React + TypeScript (SPA)** (DECISION, DEC-007/ADR-011). Ver `08c-web-architecture.md`.
- Verificación de email: **delegada a Keycloak**, sin sistema propio (DECISION, DEC-014).
- Retención/eliminación de cuenta: **soft delete con 30 días de gracia**; emails de invitados sin cuenta se purgan tras expiración/rechazo/cancelación (DECISION, DEC-015/ADR-012).
- Versiones mínimas soportadas: Android API 30 (DEC-011), iOS 17 (DEC-012), navegadores según DEC-013 (ver `03-prd.md` NFR-008).
