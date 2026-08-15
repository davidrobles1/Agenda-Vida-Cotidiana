# Vida Cotidiana — Web (WEB-001, bootstrap)

Bootstrap scaffold only — no auth, no real API consumption. That starts at WEB-002.

Stack (08c-web-architecture.md): React + TypeScript, SPA (DEC-007/ADR-011, no
SSR/Next.js in V1). ASSUMPTION: Vite as the build tool, per the doc's own TBD.
`browserslist` in `package.json` follows DEC-013.

## Token handling (not implemented yet — read before starting WEB-002)

08c-web-architecture.md leaves the OIDC token-storage pattern as an open technical
consideration (not a business decision). Given DEC-007 already fixed "SPA" (not a
Backend-for-Frontend), **Option 1 is the one to implement**: keep the token in memory
only (never `localStorage`/`sessionStorage`) and use silent renewal (`prompt=none` /
refresh token rotation) to minimize the exposure window. Do not reach for
`localStorage` "to keep it simple" — that was explicitly weighed and rejected in the
architecture doc for this reason.

## Build status (real, 2026-08-15)

Node is available in this environment, so this was run for real, not declared
NOT_EXECUTED:

```
npm install   # real, succeeded
npm run build # real, succeeded — tsc -b && vite build
```

## Structure

```
src/
  core/{api,auth,ui}
  features/{auth,home,reminders,sharing,settings}
  routes/
  App.tsx     # minimal placeholder, renders "Vida Cotidiana"
  main.tsx
```
