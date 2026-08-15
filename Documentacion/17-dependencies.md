# 17 — Versionado y dependencias

## Principios
- versiones estables;
- evitar dependencias innecesarias;
- actualizar periódicamente;
- lock/fijación donde corresponda;
- revisar CVEs;
- SBOM desde CI cuando sea práctico.

## Backend
Java 21 LTS.
Spring Boot 3.3.4 (fijado al bootstrap, ver `docs/development/00-development-baseline.md`).
Build tool: **Maven** (`Apache Maven 3.9.x` vía `./mvnw`, wrapper Maven 3.9.9). `DECISION`, ver ADR-013 (`22-decision-log.md`).

Nota histórica: el bootstrap inicial del backend (Milestone 1) se hizo con Gradle 8.9 (Kotlin DSL); se migró a Maven el 2026-08-15 por decisión explícita del Product Owner (resolución de este mismo TBD, que hasta entonces indicaba "Gradle preferido... TBD"). La migración fue de tooling únicamente — no afectó `openapi.yaml`, el modelo de datos ni ninguna decisión aprobada. Ver `docs/development/02-validation-report.md` (sección de migración) y `03-milestone-1-gate.md` (addendum) para la evidencia de revalidación real bajo Maven.

## Android
Kotlin/Compose/Android Gradle Plugin: versiones exactas `TBD` al crear el proyecto.

## Política
Dependencias críticas deben tener:
- mantenimiento activo;
- licencia compatible;
- historial razonable;
- alternativa conocida cuando sea posible.
