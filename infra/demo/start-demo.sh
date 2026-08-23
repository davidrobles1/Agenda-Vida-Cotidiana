#!/usr/bin/env bash
# Levanta el demo público (usuarios reales, red externa) sin tocar tu entorno
# de desarrollo normal (backend:8080, frontend:5173, Keycloak:8081 siguen
# intactos). Ver Documentacion/18-dev-environment.md, sección "demo público".
#
# Requisitos (una sola vez):
#   brew install ngrok caddy
#   ngrok config add-authtoken <tu-authtoken>   # dashboard.ngrok.com
#
# Uso:
#   ./infra/demo/start-demo.sh
#
# Al terminar la demo: ./infra/demo/stop-demo.sh

set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRATCH="${TMPDIR:-/tmp}/vc-demo"
mkdir -p "$SCRATCH"

KC_PORT=8091
BACKEND_PORT=8082
CADDY_PORT=8888
DEMO_DB="vidacotidiana_demo"
PG_CONTAINER="vc-dev-postgres"   # el Postgres que tu backend local ya usa (localhost:5433)
KC_ADMIN_USER=admin
KC_ADMIN_PASS=admin

log() { echo "→ $*"; }

command -v ngrok >/dev/null || { echo "falta ngrok: brew install ngrok"; exit 1; }
command -v caddy >/dev/null || { echo "falta caddy: brew install caddy"; exit 1; }

for p in $KC_PORT $BACKEND_PORT $CADDY_PORT; do
  if lsof -iTCP:"$p" -sTCP:LISTEN -n -P >/dev/null 2>&1; then
    echo "el puerto $p ya está en uso — ¿ya hay un demo corriendo? corre ./infra/demo/stop-demo.sh primero."
    exit 1
  fi
done

log "1/7 — Keycloak del demo (puerto $KC_PORT, con tema propio y ruta /auth)"
docker rm -f vc-demo-keycloak >/dev/null 2>&1 || true
docker run -d --name vc-demo-keycloak \
  -e KEYCLOAK_ADMIN="$KC_ADMIN_USER" \
  -e KEYCLOAK_ADMIN_PASSWORD="$KC_ADMIN_PASS" \
  -e KC_HTTP_RELATIVE_PATH=/auth \
  -e KC_PROXY_HEADERS=xforwarded \
  -p "$KC_PORT":8080 \
  -v "$REPO_ROOT/infra/keycloak:/opt/keycloak/data/import" \
  -v "$REPO_ROOT/infra/keycloak/themes:/opt/keycloak/themes" \
  quay.io/keycloak/keycloak:25.0 start-dev --import-realm >/dev/null

for i in $(seq 1 30); do
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$KC_PORT/auth/realms/vida-cotidiana/.well-known/openid-configuration" || true)
  [ "$code" = "200" ] && break
  sleep 2
done
[ "$code" = "200" ] || { echo "Keycloak no levantó a tiempo"; exit 1; }

log "2/7 — base de datos aislada del demo ($DEMO_DB, no toca tus datos locales)"
# Se recrea vacía en cada arranque a propósito: Keycloak arriba también nace
# vacío en cada arranque (no tiene almacenamiento persistente propio), así
# que dejar la tabla users con filas de una sesión anterior (sub distinto,
# mismo email) provoca un choque de restricción única y un 500 al primer
# login. Mantener ambos sincronizados evita ese bug — el costo es que cada
# arranque del demo empieza con cero cuentas registradas, aceptable para un
# demo de factibilidad de corta duración.
docker exec "$PG_CONTAINER" psql -U vidacotidiana -d postgres -c "DROP DATABASE IF EXISTS $DEMO_DB;" >/dev/null
docker exec "$PG_CONTAINER" psql -U vidacotidiana -d postgres -c "CREATE DATABASE $DEMO_DB;" >/dev/null

log "3/7 — túnel ngrok (dominio aleatorio gratis)"
nohup ngrok http "$CADDY_PORT" --log=stdout --log-format=logfmt > "$SCRATCH/ngrok.log" 2>&1 &
disown
for i in $(seq 1 20); do
  PUBLIC_URL=$(curl -s http://127.0.0.1:4040/api/tunnels 2>/dev/null | python3 -c "import json,sys;print(json.load(sys.stdin)['tunnels'][0]['public_url'])" 2>/dev/null || true)
  [ -n "${PUBLIC_URL:-}" ] && break
  sleep 1
done
[ -n "${PUBLIC_URL:-}" ] || { echo "ngrok no arrancó — revisa $SCRATCH/ngrok.log"; exit 1; }
log "   URL pública: $PUBLIC_URL"

log "4/7 — autorizando esa URL en el cliente web-spa de Keycloak"
KC="http://localhost:$KC_PORT"
TOKEN=$(curl -s -X POST "$KC/auth/realms/master/protocol/openid-connect/token" \
  -d "grant_type=password" -d "client_id=admin-cli" -d "username=$KC_ADMIN_USER" -d "password=$KC_ADMIN_PASS" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['access_token'])")
CLIENT_UUID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$KC/auth/admin/realms/vida-cotidiana/clients?clientId=web-spa" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)[0]['id'])")
curl -s -H "Authorization: Bearer $TOKEN" "$KC/auth/admin/realms/vida-cotidiana/clients/$CLIENT_UUID" \
  | PUBLIC_URL="$PUBLIC_URL" python3 -c "
import json, os, sys
d = json.load(sys.stdin)
url = os.environ['PUBLIC_URL']
d['redirectUris'] = list(set(d.get('redirectUris', []) + [f'{url}/callback']))
d['webOrigins'] = list(set(d.get('webOrigins', []) + [url]))
print(json.dumps(d))
" > "$SCRATCH/web-spa-update.json"
curl -s -o /dev/null -w "   client actualizado: %{http_code}\n" -X PUT \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d @"$SCRATCH/web-spa-update.json" \
  "$KC/auth/admin/realms/vida-cotidiana/clients/$CLIENT_UUID"

log "5/7 — build de producción del frontend apuntando a $PUBLIC_URL"
( cd "$REPO_ROOT/web" && \
  VITE_OIDC_ISSUER="${PUBLIC_URL}/auth/realms/vida-cotidiana" \
  VITE_OIDC_REDIRECT_URI="${PUBLIC_URL}/callback" \
  VITE_API_BASE_URL="${PUBLIC_URL}/api/v1" \
  npx vite build ) > "$SCRATCH/frontend-build.log" 2>&1 || { echo "build falló — ver $SCRATCH/frontend-build.log"; exit 1; }

log "6/7 — backend del demo (puerto $BACKEND_PORT, base de datos aislada)"
( cd "$REPO_ROOT/backend" && nohup env \
  SERVER_PORT="$BACKEND_PORT" \
  DB_URL="jdbc:postgresql://localhost:5433/$DEMO_DB" \
  OIDC_ISSUER="${PUBLIC_URL}/auth/realms/vida-cotidiana" \
  WEB_CORS_ALLOWED_ORIGINS="http://localhost:5173,${PUBLIC_URL}" \
  ./mvnw spring-boot:run > "$SCRATCH/backend-demo.log" 2>&1 & disown )
for i in $(seq 1 40); do
  grep -q "Started BackendApplication" "$SCRATCH/backend-demo.log" 2>/dev/null && break
  sleep 2
done
grep -q "Started BackendApplication" "$SCRATCH/backend-demo.log" 2>/dev/null || { echo "backend no levantó — ver $SCRATCH/backend-demo.log"; exit 1; }

log "7/7 — Caddy (enruta /auth, /api y la SPA bajo un solo dominio)"
CADDY_DEMO_WEB_DIST="$REPO_ROOT/web/dist" \
CADDY_DEMO_KEYCLOAK_UPSTREAM="localhost:$KC_PORT" \
CADDY_DEMO_BACKEND_UPSTREAM="localhost:$BACKEND_PORT" \
nohup caddy run --config "$REPO_ROOT/infra/caddy/Caddyfile.demo" --adapter caddyfile > "$SCRATCH/caddy-demo.log" 2>&1 &
disown
sleep 2

echo ""
echo "=== verificación ==="
curl -s -H "ngrok-skip-browser-warning: true" -o /dev/null -w "página principal: %{http_code}\n" "$PUBLIC_URL/"
curl -s -H "ngrok-skip-browser-warning: true" -o /dev/null -w "admin de Keycloak bloqueado (esperado 404): %{http_code}\n" "$PUBLIC_URL/auth/admin/"
echo ""
echo "Demo lista: $PUBLIC_URL"
echo "(la primera vez que alguien entre desde su navegador, ngrok le muestra una advertencia — deben darle click a \"Visit Site\")"
echo ""
echo "Para apagar todo: ./infra/demo/stop-demo.sh"
