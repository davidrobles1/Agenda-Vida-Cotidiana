# Vida Cotidiana — Documentation Index

This index distinguishes **SOURCE OF TRUTH** (the Markdown/YAML documentation that is authored, reviewed, and updated directly) from **GENERATED ARTIFACTS** (human-readable exports produced from that source for review, presentation, and handoff — never edited by hand, never authoritative).

If a generated artifact and a source document ever disagree, **the source document under `Documentacion/` wins.**

## SOURCE OF TRUTH

All of the following live under `Documentacion/` and are maintained directly.

### Governance and constitution
- `AI-CONTEXT.md` — project constitution for AI agents; concise, load-bearing context.
- `../CLAUDE.md` (repository root) — the governing rules for this documentation effort.

### Product
- `01-scope.md`, `02-roadmap.md`, `03-prd.md`, `04-use-cases.md`, `05-user-flows.md`
- `25-open-questions.md`, `26-v1-backlog.md`

### Architecture
- `06-c4.md` — C4 context/container diagrams (Mermaid)
- `07-backend-architecture.md`, `08-android-architecture.md`, `08b-ios-architecture.md`, `08c-web-architecture.md`

### Data and API contract
- `09-data-model.md` — ER diagram and data rules
- `openapi/openapi.yaml` — **canonical, machine-readable API contract** (OpenAPI 3.0.3)
- `10-api-openapi.md` — narrative companion to the API contract
- `openapi/API-DOCS-GENERATION.md` — how generated API documentation would be derived from `openapi.yaml`

### Security documentation
- `11-auth-security.md`, `21-security.md`

### Decision records
- `22-decision-log.md` — ADR-001 to ADR-012 (Architecture Decision Records)
- `28-v1-decision-pack.md` — the 15 approved V1 decisions (DEC-001 to DEC-015)

### Traceability and acceptance
- `12-traceability.md`, `13-acceptance.md`

### Process
- `14-definition-of-done.md`, `15-coding-standards.md`, `16-git-pr.md`, `17-dependencies.md`, `18-dev-environment.md`, `19-cicd.md`

### Readiness and consistency reviews
- `27-v1-readiness-review.md` — initial V1 readiness review (identified 5 blockers)
- `29-v1-final-readiness.md` — readiness after the 15 decisions closed (`V1_READINESS_STATUS: READY`)
- `30-documentation-consistency-review.md` — consistency review performed before generating this documentation pack
- `31-documentation-pack-audit.md` — final audit of this generated pack
- `32-v1-development-gate-audit.md` — V1 development gate audit: an external (Gemini) findings report validated against the decision log and corrected at the source; `V1_READINESS_STATUS: READY`

## GENERATED ARTIFACTS

Everything under `docs/generated/` is produced **from** the source documentation above, for review, presentation, and handoff purposes. These files are regenerated, not hand-edited, and supersede no source document.

Each deliverable exists in two languages, as separate files (`-EN` / `-ES` suffix). Both are generated from the same underlying source documentation and are content-equivalent — neither is more authoritative than the other; `Documentacion/` (in Spanish) remains the single source of truth for both.

| File | Derived from | Purpose |
|---|---|---|
| `docs/generated/V1-Software-Architecture-and-Product-Specification-EN.docx` | All 22 authoritative source documents listed above | Full cross-referenced technical specification (32 sections), English |
| `docs/generated/V1-Software-Architecture-and-Product-Specification-ES.docx` | Same source documents | Same specification, Spanish |
| `docs/generated/V1-Software-Architecture-and-Product-Specification-EN.pdf` | Same content as the English `.docx` above | PDF rendering, English, for distribution/archival |
| `docs/generated/V1-Software-Architecture-and-Product-Specification-ES.pdf` | Same content as the Spanish `.docx` above | PDF rendering, Spanish, for distribution/archival |
| `docs/generated/V1-Product-and-Architecture-Overview-EN.pptx` | Condensed from the same source documents | Executive presentation (27 slides), English — no invented metrics, KPIs, or business claims; every figure traces back to a source document |
| `docs/generated/V1-Product-and-Architecture-Overview-ES.pptx` | Same source documents | Same executive presentation, Spanish |

No API documentation (Redoc/Swagger UI output) has been generated yet — see `Documentacion/openapi/API-DOCS-GENERATION.md` for the recommended process once the backend project exists.

## Addendum — Módulo Laboral (ADR-016, 2026-08-22)

Propuesta de evolución del modo Laboral (ADR-015) hacia un espacio profesional universal (Personas/Proyectos/Compromisos), aprobada por el Product Owner. No forma parte del pack V1 original de arriba; se documenta por separado porque V1 ya estaba cerrado cuando se aprobó.

- **Fuente de verdad:** `Documentacion/22-decision-log.md` ADR-016; `Documentacion/34-laboral-module-proposal.md` (análisis completo); `Documentacion/03-prd.md` FR-021 a FR-028/NFR-011; `Documentacion/09-data-model.md` §"V3 — Módulo Laboral"; `Documentacion/11-auth-security.md` SEC-004; `Documentacion/04-use-cases.md` UC-17 a UC-21; `Documentacion/05-user-flows.md`.
- **Plan de trabajo:** `docs/development/08-laboral-module-plan.md`.
- **Generado:** `docs/generated/Modulo-Laboral-Propuesta-V3-ES.pptx` — resumen ejecutivo condensado de `34-laboral-module-proposal.md`, mismo criterio que el pack V1 (sin métricas/KPIs/cifras de negocio inventadas, todo dato trazable a un documento fuente).

## Regenerating the artifacts

The generated artifacts were produced by scripts (`pptxgenjs` for the deck, the `docx` npm package for the Word document) that read the current state of the source Markdown/YAML files. There is no automated regeneration pipeline yet — this is a manual step, run whenever the source documentation changes materially enough to warrant a refreshed handoff package. Regenerating does not modify any file under `Documentacion/`.
