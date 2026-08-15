# Vida Cotidiana — Documentación del Proyecto

Repositorio de documentación para construir Vida Cotidiana de forma incremental.

## Principios
- V1: MVP funcional, mínimo alcance.
- V2: estabilización y endurecimiento.
- V3: mejoras funcionales, rendimiento y escalabilidad.
- V4: versión final objetivo.
- IA: fase posterior a V4.
- Finanzas: fase posterior a V4.
- No se consideran requerimientos de negocio no confirmados.
- `TBD` identifica una decisión pendiente.
- Seguridad y privacidad se diseñan desde V1, pero sin sobrearquitectura.

## Stack base propuesto
- Plataformas V1: Android, iOS y Web (ADR-005), mismo backend/API para las tres.
- Android: Kotlin + Jetpack Compose (minSdk 30, DEC-011).
- iOS: Swift + SwiftUI (DEC-006/ADR-010, iOS mínimo 17, DEC-012).
- Web: React + TypeScript, SPA (DEC-007/ADR-011).
- Arquitectura Android/iOS: Clean Architecture + MVVM con flujo unidireccional; MVI solo donde aporte valor.
- Backend: Java 21 LTS + Spring Boot 3.x.
- Arquitectura backend: monolito modular.
- Base de datos: PostgreSQL.
- Identidad: **Keycloak** (OIDC/OAuth 2.1 self-hosted, DEC-004/ADR-008); verificación de email delegada a Keycloak (DEC-014).
- Recordatorios compartibles desde V1: owner + colaboradores vía invitación (ADR-006), estado de completado único global (DEC-001), eliminación en cascada con notificación (DEC-002).
- Notificaciones: locales + push vía **Firebase Cloud Messaging** (DEC-010/ADR-007), con `DEVICE_PUSH_TOKEN` multi-dispositivo (DEC-005).
- Archivos: object storage compatible con S3 (preparado conceptualmente, sin implementar en V1).
- API: REST + OpenAPI.
- CI/CD: GitHub Actions.
- Cloud: **servidor propio alquilado (self-hosted)** (DEC-008/ADR-014; corrige la decisión previa de AWS/ADR-009 del 2026-08-09). Correo transaccional: `TBD` (DEC-009 reabierta).
- Retención/eliminación de cuenta: soft delete con 30 días de gracia (DEC-015/ADR-012).
- IA: fuera de V1–V4.
- Finanzas: fuera de V1–V4.

## Estado
Las 15 decisiones bloqueantes identificadas en `27-v1-readiness-review.md` quedaron cerradas en `28-v1-decision-pack.md` (2026-08-09) y reflejadas en `22-decision-log.md`, `09-data-model.md` y `openapi/openapi.yaml`. Ver `29-v1-final-readiness.md` para el estado final de preparación de V1 y los TBDs no bloqueantes restantes.
