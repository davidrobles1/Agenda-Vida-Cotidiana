# API Docs Generation — how generated API documentation would be derived from `openapi.yaml`

**Canonical source:** `Documentacion/openapi/openapi.yaml` (OpenAPI 3.0.3). This file is the single API contract. It is not duplicated, manually re-typed, or rewritten into another YAML/JSON file anywhere in this repository. `10-api-openapi.md` is a narrative companion document for humans, not a second contract — if the two ever disagree, `openapi.yaml` wins (see `30-documentation-consistency-review.md`, finding E, for one place where they currently drift and have not yet been reconciled).

## Status: no API-docs generation tooling exists in this repository yet

No Redoc, Swagger UI, or OpenAPI-Generator configuration has been added to this repository. This document describes the intended process for when the backend project is bootstrapped — it is a **RECOMMENDATION**, not something implemented in this documentation cycle, per CLAUDE.md's rule against adding tooling/dependencies without justification and without it being requested.

## Recommended approach when the backend project is bootstrapped

1. **Human-browsable API reference — Redoc or Swagger UI.**
   Both tools render an HTML page directly from `openapi.yaml`, with no manual transcription:
   - Redoc (static, CI-friendly): `npx @redocly/cli build-docs Documentacion/openapi/openapi.yaml -o docs/generated/api-reference.html`
   - Swagger UI (interactive, "Try it out" console): serve `openapi.yaml` behind `swagger-ui-express` (or an equivalent for the chosen stack) pointed at the same file.
   Either output belongs under `docs/generated/` (generated artifact), never hand-edited.

2. **Contract validation in CI.**
   Lint `openapi.yaml` on every change with `@redocly/cli lint` or `openapi-spec-validator`, so drift between the contract and the narrative docs is caught automatically rather than discovered later (as happened with finding E in `30-documentation-consistency-review.md`).

3. **Server/client code generation (optional, later version).**
   Tools such as `openapi-generator` or `orval` can generate typed HTTP clients (e.g. for the React Web SPA) or server stubs directly from `openapi.yaml`. This is not required for V1 and should only be introduced when it removes real duplication of effort — consistent with CLAUDE.md's "no sobrearquitecturar" rule.

## What this means for this generated documentation pack

- `docs/generated/V1-Software-Architecture-and-Product-Specification.docx/.pdf` includes a narrative, table-based summary of the API surface (Section 23, "API Architecture") for readability, sourced from `openapi.yaml` and `10-api-openapi.md`. It is a **derived, generated artifact** — it does not replace `openapi.yaml` as the contract, and it is not itself a second machine-readable API spec.
- No new OpenAPI file was created. No endpoint was invented beyond what `openapi.yaml` already defines.

## Known gaps in `openapi.yaml` (pre-existing, not introduced by this documentation pack)

Carried over from `29-v1-final-readiness.md` §4 and `30-documentation-consistency-review.md`, listed here for visibility, not resolved in this cycle:
- No `securitySchemes` / OIDC block defined yet.
- No reusable `Error` schema.
- No `requestBody` defined for `POST`/`PATCH /reminders`.
- `Reminder`, `Invitation`, `ReminderShare` schemas exist under `components.schemas` but are not yet referenced via `$ref` in any path response.

These are recommended to be resolved before generating server/client code from the spec (item 3 above), but do not block starting V1 domain development.
