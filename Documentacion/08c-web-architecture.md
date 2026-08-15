# 08c — Arquitectura Web

**DECISION (DEC-007/ADR-011):** React + TypeScript como SPA (sin SSR/Next.js en V1). Justificación: V1 requiere sesión autenticada, con poco valor de SEO público confirmado; una SPA pura es suficiente y más simple de operar.

**DECISION (DEC-013):** navegadores soportados: últimas 2 versiones mayores de Chrome, Edge y Firefox (desktop), Safari desktop (actual y anterior), Safari iOS (actual y anterior), Chrome Android (actual).

## Stack
- React + TypeScript.
- Cliente HTTP (p. ej. fetch nativo o una librería ligera) para consumir la API REST `/api/v1`.
- Librería OIDC para SPA (Authorization Code + PKCE) contra Keycloak (DEC-004).
- Firebase SDK (Web Push) para push (DEC-010).
- `browserslist` configurado según DEC-013 para el build/transpilación.

## Patrón
Estructura por features, análoga conceptualmente a Android/iOS (sin compartir código):
```text
src/
  core/
    api/
    auth/
    ui/
  features/
    auth/
    home/
    reminders/
    sharing/
    settings/
  routes/
```

## Seguridad — nota pendiente no bloqueante

**Consideración técnica abierta (no es una decisión de negocio, no bloquea V1):** al ser una SPA pública, el manejo de los tokens OIDC en el navegador requiere definir un patrón concreto antes de implementar el cliente Web:
- **Opción 1:** almacenar el token en memoria (no en `localStorage`/`sessionStorage`) y usar renovación silenciosa (`prompt=none` / refresh token rotation), minimizando la ventana de exposición.
- **Opción 2:** patrón Backend-for-Frontend (BFF) con cookie `httpOnly`, evitando que el token toque JavaScript del navegador — más seguro, pero añade un componente de backend adicional (fuera del alcance decidido para V1, que es "SPA" según DEC-007).

Dado que DEC-007 fijó explícitamente "SPA" (no BFF), la Opción 1 es la más coherente con la decisión ya tomada; se documenta aquí como pendiente de definición técnica de implementación, no como una nueva decisión de negocio a aprobar.

## Push (DEC-005/DEC-010)
El cliente Web solicita permiso de notificaciones, obtiene un token de Web Push vía Firebase y lo registra contra el backend (`POST /me/devices`, FR-012).

## TBD
- Herramienta de build (Vite asumido por defecto; alternativa TBD si el equipo prefiere otra).
- Librería de gestión de estado (a definir en el bootstrap; no se especifica en este documento para no sobrearquitecturar antes de tener el proyecto creado).
