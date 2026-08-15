# 02 — Roadmap por versiones

| Versión | Objetivo | Resultado |
|---|---|---|
| V1 | MVP | Aplicación funcional con cuenta + Home + tareas/recordatorios |
| V2 | Estabilidad | Producto confiable, seguro y medible |
| V3 | Evolución | Nuevas funciones + rendimiento + escalabilidad |
| V4 | Final | Alcance completo aprobado |
| Post-V4 | IA/Finanzas | Fases separadas, sujetas a validación |

## V1 — Definition of Success
- usuario puede registrarse/iniciar sesión en Android, iOS y Web;
- puede crear y completar un recordatorio;
- puede compartir un recordatorio con una o varias personas mediante invitación, y estas pueden aceptar/rechazar/ver/completar según su permiso (ver ADR-006);
- recibe notificaciones locales y push para recordatorios propios y compartidos (ver ADR-007);
- datos quedan aislados por usuario y por relación de compartición explícita (owner vs. colaborador);
- aplicación maneja errores de red;
- backend tiene pruebas automatizadas;
- CI bloquea cambios que fallen compilación/pruebas;
- no existen secretos en repositorio;
- OpenAPI está versionado.

## Regla de evolución
Una versión no debe requerir rehacer el dominio anterior. Los módulos se agregan detrás de contratos estables.
