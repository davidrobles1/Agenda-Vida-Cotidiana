#!/usr/bin/env bash
# Apaga todo lo que start-demo.sh levantó. Tu entorno de desarrollo normal
# (backend:8080, frontend:5173, Keycloak:8081) no se toca — nunca lo usó.

pkill -f "ngrok http" 2>/dev/null && echo "túnel ngrok detenido" || echo "ngrok ya estaba apagado"
pkill -f "caddy run --config .*Caddyfile.demo" 2>/dev/null && echo "Caddy detenido" || echo "Caddy ya estaba apagado"

BACKEND_PID=$(lsof -tiTCP:8082 -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$BACKEND_PID" ]; then kill "$BACKEND_PID" && echo "backend del demo detenido"; else echo "backend del demo ya estaba apagado"; fi

# Se DETIENE, no se elimina (requisito del Product Owner, 2026-09-23): las
# cuentas del demo deben conservarse entre sesiones. `docker stop` preserva el
# contenedor y su volumen de datos; `docker rm` no debe usarse aquí.
docker stop vc-demo-keycloak >/dev/null 2>&1 && echo "Keycloak del demo detenido (contenedor y usuarios conservados)" || echo "Keycloak del demo ya estaba apagado"

echo ""
echo "Listo — nada del demo sigue expuesto a internet."
echo "Nada se borró: las cuentas de Keycloak (volumen 'vc-demo-keycloak-data') y"
echo "los datos de la base 'vidacotidiana_demo' se conservan, y el próximo"
echo "./infra/demo/start-demo.sh los reutiliza tal cual."
