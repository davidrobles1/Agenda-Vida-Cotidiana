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
Spring Boot 3.x: versión exacta `TBD` al bootstrap.
Gradle preferido para build: `TBD`.

## Android
Kotlin/Compose/Android Gradle Plugin: versiones exactas `TBD` al crear el proyecto.

## Política
Dependencias críticas deben tener:
- mantenimiento activo;
- licencia compatible;
- historial razonable;
- alternativa conocida cuando sea posible.
