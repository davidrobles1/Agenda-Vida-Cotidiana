# 19 — CI/CD

**ASSUMPTION (INFRA-003, añadida el 2026-08-15):** el pipeline se implementó como GitHub Actions
(`.github/workflows/backend-ci.yml`). Esto es una **suposición técnica, no una decisión de negocio
aprobada**: GitHub es el único nombre relacionado con CI/organización que aparece en la
documentación (`25-open-questions.md`, pregunta 4 — "¿Licencia/nombre de repositorio y organización
GitHub?" — sigue abierta, no decidida). Si el equipo termina usando otro proveedor de CI, este
workflow debe migrarse; no debe tratarse como DECISION.

El escaneo de dependencias (`dependency/security scan`) usa **GitHub Dependabot**
(`.github/dependabot.yml`), no el plugin `owasp-dependency-check-maven`: añadir ese plugin a
`pom.xml` habría introducido una dependencia de red a la base de datos NVD en cada build (latencia y
rate-limiting sin API key), arriesgando un pipeline lento/inestable para un beneficio equivalente al
que ya da Dependabot sin tocar el build de Maven.

**Paso manual pendiente (no versionable):** las branch protection rules (exigir que
`backend-ci.yml` pase y exigir revisión antes de mergear a `main`) son configuración del
repositorio remoto en GitHub, no un archivo versionable en este repo — deben activarse manualmente
una vez exista la organización/repositorio GitHub definitivo (ver pregunta 4 en
`25-open-questions.md`).

## Pull Request
1. checkout;
2. dependency restore;
3. compile;
4. unit tests;
5. integration tests;
6. static analysis;
7. dependency/security scan;
8. secret scan;
9. build artifact.

## Main
- todo lo anterior;
- build reproducible;
- artifact versionado;
- deploy a staging: TBD.

## Producción
Requiere aprobación y controles adicionales: TBD.

## Security gates V1
- secret scanning;
- dependency vulnerability scan;
- tests;
- SAST básico;
- branch protection.
