#!/usr/bin/env bash
# Apaga todo lo que start-demo.sh levantó. Tu entorno de desarrollo normal
# (backend:8080, frontend:5173, Keycloak:8081) no se toca — nunca lo usó.

pkill -f "ngrok http" 2>/dev/null && echo "túnel ngrok detenido" || echo "ngrok ya estaba apagado"
pkill -f "caddy run --config .*Caddyfile.demo" 2>/dev/null && echo "Caddy detenido" || echo "Caddy ya estaba apagado"

BACKEND_PID=$(lsof -tiTCP:8082 -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$BACKEND_PID" ]; then kill "$BACKEND_PID" && echo "backend del demo detenido"; else echo "backend del demo ya estaba apagado"; fi

docker rm -f vc-demo-keycloak >/dev/null 2>&1 && echo "contenedor de Keycloak del demo eliminado" || echo "Keycloak del demo ya estaba apagado"

echo ""
echo "Listo — nada del demo sigue expuesto a internet."
echo "La base 'vidacotidiana_demo' queda en disco para que puedas revisar los datos"
echo "si quieres, pero start-demo.sh la borra y recrea vacía en el próximo arranque"
echo "(Keycloak tampoco conserva usuarios entre arranques, así que ambos se resetean juntos)."
