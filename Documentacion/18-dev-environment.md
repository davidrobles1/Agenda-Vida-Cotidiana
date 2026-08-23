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
Build tool: Maven (`./mvnw`, ver ADR-013 en `22-decision-log.md`). No requiere Maven instalado localmente — el wrapper lo descarga.
```bash
./mvnw test
./mvnw spring-boot:run
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

## Exposición fuera de la LAN (testers / demo público)

ASSUMPTION: esta sección cubre necesidades puntuales de acceso desde otras
redes durante desarrollo (no reemplaza el despliegue real en el servidor
rentado — ver DEC-008/ADR-014, `22-decision-log.md`). El stack sigue siendo
`start-dev` de Keycloak sin backups: no usar para datos que deban persistir.

La causa raíz de por qué "simplemente cambiar la IP" no basta: Keycloak
valida `redirectUris`/`webOrigins` del cliente `web-spa` contra una lista
fija (`infra/keycloak/realm-vida-cotidiana.json`), y el `iss` del token debe
coincidir exactamente entre lo que ve el navegador, el frontend
(`VITE_OIDC_ISSUER`) y el backend (`OIDC_ISSUER`). Los tres deben apuntar a
la MISMA dirección pública.

### Caso 1 — testers conocidos, sin usuarios reales

RECOMMENDATION: Tailscale (mesh VPN, gratis) en vez de exponer nada a
internet. Cada dispositivo (host + testers) se une a la misma tailnet; se usa
el hostname MagicDNS del host (estable) en lugar de la IP de LAN en los 3
puntos de configuración (Keycloak client, `OIDC_ISSUER`/`WEB_CORS_ALLOWED_ORIGINS`
del backend, `VITE_OIDC_ISSUER`/`VITE_API_BASE_URL` del frontend).

### Caso 2 — demo público controlado (usuarios reales, sin dominio propio)

RECOMMENDATION: un solo hostname público (dominio aleatorio gratis de ngrok)
al frente de un reverse proxy local (Caddy) que enruta por path a los 3
servicios (`infra/caddy/Caddyfile.demo`), para no necesitar comprar dominio ni
levantar 3 túneles. Bloquea `/auth/admin/*` a propósito — la consola admin de
Keycloak nunca se expone por el túnel, solo accesible en `localhost:8091/admin`.

Automatizado en dos scripts (`infra/demo/start-demo.sh` y `stop-demo.sh`),
probados de punta a punta (login real + llamada a API protegida) tras
encontrar y corregir en la práctica:
- `try_files` debe ir **antes** que `file_server` en el Caddyfile — al revés,
  cualquier ruta de la SPA que no sea `/` (como `/callback`) devuelve 404 en
  vez de servir `index.html`, y el login nunca se completa.
- El volumen del theme (`infra/keycloak/themes`) debe montarse junto al de
  importación del realm, o se ve el login genérico de Keycloak.
- `VITE_OIDC_REDIRECT_URI` es obligatorio en el build — sin él cae al default
  `http://localhost:5173/callback` y Keycloak redirige ahí en vez de a la URL
  pública.
- Keycloak (modo `start-dev`, sin volumen propio) y la base de datos del demo
  deben resetearse **juntos** en cada arranque: si Keycloak nace vacío pero la
  tabla `users` conserva filas de una sesión anterior, el mismo email con un
  `sub` distinto choca contra `uq_users_email` y tira 500 en el primer login.

Requisitos (una sola vez):
```bash
brew install ngrok caddy
ngrok config add-authtoken <tu-authtoken>   # dashboard.ngrok.com, plan gratis
```

Uso:
```bash
./infra/demo/start-demo.sh   # levanta todo, imprime la URL pública al final
./infra/demo/stop-demo.sh    # apaga todo; tu entorno normal (8080/5173/8081) no se toca
```

`start-demo.sh` es idempotente y se puede correr las veces que haga falta
(cada arranque = URL pública nueva de ngrok, ya que el plan gratis no permite
dominio fijo — hay que reavisar la nueva URL a los testers cada vez que se
reinicia). Cada arranque también borra y recrea vacías las cuentas del demo
(Keycloak + `vidacotidiana_demo`) — aceptable para un demo de factibilidad de
corta duración, no pensado para acumular usuarios reales entre sesiones.

DOCUMENTATION_CONFLICT: el realm (`realm-vida-cotidiana.json`) tiene
`resetPasswordAllowed: false` y no hay servidor de correo configurado
(`EMAIL_URL` solo existe para GlitchTip). Un usuario real de la demo que
olvide su contraseña no tiene forma de recuperarla — avisar a los
participantes o restablecer manualmente vía Admin Console mientras esto no
se resuelva (ver PRD/requisitos de auth para decidir si V1 requiere reset
por email).
