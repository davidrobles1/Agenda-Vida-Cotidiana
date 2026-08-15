# 16 — Git y Pull Requests

## Branches
- `main`: estable.
- `develop`: TBD; preferencia inicial: evitarla si no aporta valor.
- `feature/*`
- `fix/*`
- `security/*`
- `chore/*`

Recomendación V1: trunk-based simplificado con PR hacia `main`.

## Commit
Conventional Commits:
- feat
- fix
- docs
- test
- refactor
- chore
- security

## PR
Debe incluir:
- objetivo;
- cambios;
- pruebas;
- riesgos;
- migraciones;
- evidencia visual para UI;
- ticket/requisito;
- impacto de seguridad.

## Regla
No merge directo a main.
CI obligatoria.
Al menos un reviewer: TBD.
