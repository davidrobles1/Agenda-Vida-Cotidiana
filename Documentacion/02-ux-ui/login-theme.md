# Login theme (Keycloak)

**Version:** V1
**Status:** RECOMMENDATION (implemented and verified for real — Android device + Web/Playwright, see §6). Not an ADR: it is a visual layer over the identity-provider decision already made in `11-auth-security.md`/`22-decision-log.md` (ADR-008), not a new architectural decision.

## 1. Why the login page has its own palette

`Documentacion/02-ux-ui/design-system.md` defines the app's tokens for Android/Web screens we render ourselves. The Keycloak login page is different: it's server-rendered HTML owned by Keycloak, reached via a Custom Tab (Android) or a full-page redirect (Web), outside both the Compose and the Vite build. Giving it "its own palette" doesn't mean a different brand — it reuses the exact same primary indigo (`#4F46E5`) and the same touch-target/contrast rules from `design-system.md`/`accessibility.md`. What's different is only the delivery mechanism (Keycloak theme, not app code) and, deliberately, the layout per client (§3).

This is coherence, not a `DOCUMENTATION_CONFLICT`: one visual language, two renderers (the app; Keycloak).

## 2. Mechanism

Two standard Keycloak login themes, no plugin:

```
infra/keycloak/themes/
├── vida-cotidiana-mobile/login/
│   ├── theme.properties   (parent=keycloak.v2)
│   ├── login.ftl          (real base + one brand-mark line)
│   └── resources/css/login.css
└── vida-cotidiana-web/login/
    ├── theme.properties   (parent=keycloak.v2)
    ├── login.ftl          (same, one brand-mark line)
    ├── template.ftl       (real base + one <aside> hero panel)
    └── resources/css/login.css
```

**RECOMMENDATION (verified, not guessed):** every `.ftl` file here started as a byte-for-byte copy extracted from the actual `quay.io/keycloak/keycloak:25.0` image's bundled `keycloak.v2` theme (`org.keycloak.keycloak-themes-25.0.6.jar`), not written from memory. `parent=keycloak.v2` means everything NOT overridden here (every other page: register, OTP, error, password reset, WebAuthn...) falls back to the real, working Keycloak default. Only `login.ftl` (one added `<div class="vc-brand">`) and, for the web theme only, `template.ftl` (one added `<aside class="vc-hero" aria-hidden="true">`) are touched. This was a deliberate risk-minimization choice: the brief itself flagged that a malformed FreeMarker theme can break the Authorization Code + PKCE flow (missing macro, broken `<#nested>` call), so the smallest possible diff against a known-working file was preferred over writing a theme from scratch.

`docker-compose.yml` mounts `./infra/keycloak/themes` at `/opt/keycloak/themes` (confirmed empty except a README in the base image before mounting — verified against the running container, not assumed). `attributes.login_theme` on each client (`android-app`/`ios-app` → `vida-cotidiana-mobile`, `web-spa` → `vida-cotidiana-web`) selects the theme per client — a real Keycloak client attribute, confirmed against the live Admin REST API's error response after an initial wrong guess (`loginTheme` as a top-level field doesn't exist on `ClientRepresentation`; corrected to `attributes.login_theme`, see §6).

## 3. Layout: mobile (single column) vs. web (split panel)

- **Mobile** (`vida-cotidiana-mobile`): single-column card, matching the narrow Custom Tab viewport (phone width) it's actually rendered in. A two-column layout would just collapse into one anyway — designing for one column from the start avoids a pointless breakpoint.
- **Web** (`vida-cotidiana-web`): split panel — the real login form on the left (`min(45vw, 480px)`), a decorative indigo gradient hero panel with the "Vida Cotidiana" wordmark and a one-line tagline on the right (`1fr`). Collapses to a single column under 860px, since the login page is reachable standalone (a bookmarked `/realms/...` URL), even though `platform-guidelines.md` doesn't otherwise treat web-spa as a responsive target.

The hero panel is `aria-hidden="true"`: it's a decorative duplicate of branding that's already conveyed by the real, accessible `<h1>` inside the form column (rendered by the inherited `template.ftl` header section), so hiding it from assistive tech is correct, not a gap — there is no unique content inside it that isn't otherwise available.

## 4. Typography — RECOMMENDATION, deliberately not Inter

`design-system.md` self-hosts Inter for the Web SPA via `@fontsource/inter`, inside the Vite build. The Keycloak login page is not part of that build — bundling font binaries a second time into a Keycloak theme, for a page most users see rarely (SSO usually keeps them signed in), wasn't judged worth the maintenance weight. Both login themes use a system-font stack instead (`-apple-system, Roboto, 'Segoe UI', system-ui, sans-serif` on mobile; `system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif` on web) — declared here explicitly rather than silently degrading. If this is revisited, FUTURE: self-host Inter under `resources/fonts/` in the web theme specifically.

## 5. Accessibility

- Touch targets: 48px minimum on the mobile theme's inputs/button (`accessibility.md`'s Android 48dp rule — this page runs inside an Android Custom Tab), 44px on the web theme (WCAG 2.5.5, matching `web/src/index.css`).
- Color contrast: reuses the same primary/error tokens already verified for AA in `design-system.md`/`accessibility.md` (button text on `#4F46E5`, error text on `#fee2e2` container) — not re-derived here.
- The one non-inherited structural change (the web theme's `<aside>`) is `aria-hidden`, per §3.
- Everything else — labels, `aria-invalid`, the password-visibility toggle, error announcement (`aria-live="polite"` on `#input-error`) — is inherited unchanged from `keycloak.v2`, since `login.ftl`/`template.ftl` were only ever appended to, never rewritten.

## 6. Real bug found and fixed during verification

Building the web split panel with a plain `grid-template-columns: min(45vw, 480px) 1fr` produced a layout where the hero panel didn't line up with the math: `getComputedStyle` showed `480px 700px` instead of `480px 960px`, and `.pf-v5-c-login__main` reported a 356px height instead of stretching to the viewport. Root cause, confirmed via `getComputedStyle`/`boundingBox()` in a throwaway Playwright script (not by re-eyeballing the screenshot): the inherited `keycloak.v2` base theme sets its own padding and a 64px `column-gap` on `.pf-v5-c-login__container` for its own (unused-by-us) internal responsive grid, and never sets `box-sizing: border-box`. Fixed by explicitly zeroing `gap`/`padding`/`margin` and setting `box-sizing: border-box` under the `vc-theme-web` scope, plus `align-self: stretch; height: 100%` on the form column. Re-verified with the same computed-style check before treating it as done (`main` now reports `x:0 width:480`, `hero` reports `x:480 width:960`, no gap).

A second real correction happened one layer down, in the realm JSON / Admin API itself: the client attribute is `attributes.login_theme`, not a top-level `loginTheme` field — the first attempt (`loginTheme` at the top level) was rejected by the live Keycloak Admin REST API with `Unrecognized field "loginTheme"`, which is how this was caught and corrected, rather than trusting the initial (wrong) assumption from documentation search.

## 7. Verification performed

- Both themes render `200` from the real running Keycloak (`vc-dev-keycloak`, not simulated), with no FreeMarker errors, and correctly resolve both their own `css/login.css` and the inherited `css/styles.css`.
- Android: real Custom Tab login on a physical Samsung device, screenshots in `screenshots/android-kc-login-before.png` (stock `keycloak.v2`, `login_theme` attribute temporarily cleared) and `screenshots/android-kc-login-after.png` (themed). `LoginAndRemindersFlowTest` and `SharingFlowTest` (real instrumented tests, real device) both pass against the themed page.
- Web: real Playwright/Chromium capture, `screenshots/web-kc-login-before.png`/`web-kc-login-after.png` (same temporarily-cleared-then-restored technique via the Admin API, mirroring the git-stash technique used for the app-level restyle in the prior task). `sharing.spec.ts`, `notifications.spec.ts` and `error-tracking.spec.ts` — each of which logs in through this exact real page as setup — all pass against the themed page.
- The live Keycloak dev container (`vc-dev-keycloak`) was patched in place via `docker cp` (theme files) + the Admin REST API (`attributes.login_theme`) rather than recreated from the updated realm JSON — recreating it would have wiped test users (`testuser`, `userb`) created outside the realm-export JSON, whose Keycloak `sub` IDs are referenced by owner columns in the backend's Postgres data. The checked-in `realm-vida-cotidiana.json` carries the same `attributes.login_theme` values, so a clean `docker compose up` (fresh volume) reproduces the same result; `realm-vida-cotidiana-test.json` has no `android-app`/`ios-app`/`web-spa` clients (it's used for backend token-validation tests only) and was left untouched.

## 8. Visual reference and what wasn't attempted

No pixel-exact reference image was available to this pass to replicate 1:1 — this section documents that honestly rather than claiming a fidelity that wasn't verified against anything.

**DECISION (scope, both themes):** no photographic imagery. Reason: cost/licensing wasn't approved for V1 (a real photo asset needs either a paid license or a curated free source, neither of which is a decision to make unilaterally), and it would also add an external/bundled binary asset to a theme that's otherwise entirely text + CSS + inline markup.

**DECISION (web theme only):** the hero panel's decorative shapes (`.vc-hero-ornament`) are hand-written inline SVG — three overlapping soft circles at low opacity over the gradient (`template.ftl`) — not an external asset, not an icon library, and explicitly **not a logo or brand mark**: no such asset has been designed or approved anywhere in `Documentacion/02-ux-ui/`, and inventing one here would be a product/brand decision this task isn't authorized to make silently (CLAUDE.md: no inventar requerimientos de negocio). If the project later approves a real logo/icon set, this ornament is the one piece of this theme meant to be swapped, not extended.

**RECOMMENDATION (mobile theme):** stays ornament-free, on purpose — see §3's rationale (narrow Custom Tab viewport; a card at that width has no room for a background flourish without crowding the form). Not an oversight.

If a specific visual reference exists for this login page, it needs to be shared for a follow-up pass — this section will be updated against it at that point, rather than the abstract circles being presented as a final design.

## 9. iOS

**FUTURE (not blocked, no code needed):** `ios-app`'s `attributes.login_theme` is already set to `vida-cotidiana-mobile` in the realm config — the same theme Android uses, since both are narrow-viewport Custom-Tab/`ASWebAuthenticationSession`-style contexts. This is realm/server-side configuration; when iOS login (`IOS-002`) resumes, it will pick up the themed page automatically, with no iOS-side app changes required. Not verified on an iOS device in this task (no device driven here) — verify visually once `IOS-002` actually reaches its login screen.
