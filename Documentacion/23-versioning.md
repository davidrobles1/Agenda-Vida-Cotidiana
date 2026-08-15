# 23 — Versionado del producto

## App
SemVer conceptual:
`MAJOR.MINOR.PATCH`

V1 puede publicarse inicialmente como `1.0.0`.

## API
Versionado mayor en URL:
`/api/v1`

Cambios compatibles no cambian `v1`.
Cambios incompatibles requieren `v2`.

## Database
Migraciones Flyway:
`V1__initial_schema.sql`

No editar migraciones ya ejecutadas.
