# 18 — Entorno de desarrollo

## Requisitos
- JDK 21.
- Android Studio estable.
- Android SDK.
- Git.
- Docker Desktop/Engine.
- PostgreSQL local mediante Docker.
- IDE con plugins Kotlin/Java.

## Backend
```bash
./gradlew test
./gradlew bootRun
```

## Android
```bash
./gradlew test
./gradlew connectedCheck
```

## Servicios locales V1
- PostgreSQL.
- backend.
- Android emulator/device.

Redis, Kafka, Kubernetes y otros componentes NO se agregan a V1 sin requerimiento.

## Configuración
Usar `.env` solo para desarrollo local y nunca versionar secretos reales.
Ejemplo:
```text
DB_URL=
DB_USERNAME=
DB_PASSWORD=
OIDC_ISSUER=
```
