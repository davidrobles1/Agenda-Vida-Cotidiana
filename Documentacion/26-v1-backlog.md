# 26 — Backlog V1

| ID | Historia | Prioridad | Dependencia |
|---|---|---|---|
| US-001 | Como visitante quiero registrarme | Must | Identity |
| US-002 | Como usuario quiero iniciar sesión | Must | Identity |
| US-003 | Como usuario quiero ver Home | Must | API |
| US-004 | Como usuario quiero crear un recordatorio | Must | DB/API |
| US-005 | Como usuario quiero editar un recordatorio | Must | US-004 |
| US-006 | Como usuario quiero completarlo | Must | US-004 |
| US-007 | Como usuario quiero eliminarlo | Must | US-004 |
| US-008 | Como usuario quiero recibir aviso local | Should | US-004 |
| US-009 | Como usuario quiero cerrar sesión | Must | Identity |
| US-010 | Como usuario quiero que otro usuario no pueda acceder a mis datos | Must | Authorization |
| US-011 | Como propietario quiero compartir un recordatorio con una o varias personas mediante invitación | Must | Sharing/Identity |
| US-012 | Como destinatario quiero aceptar o rechazar una invitación de compartición | Must | US-011 |
| US-013 | Como propietario quiero revocar el acceso de un colaborador en cualquier momento | Must | US-011 |
| US-014 | Como usuario quiero recibir una notificación push cuando ocurra un evento relevante de compartición | Should | Push provider |
| US-015 | Como usuario quiero usar la app de forma coherente en Android, iOS y Web | Must | Identity/API |
| US-016 | Como usuario quiero poder solicitar la eliminación de mi cuenta | Must | Identity/Account (FR-013, DEC-015) |
| US-017 | Como usuario quiero que mi dispositivo quede registrado para recibir notificaciones push | Should | Notification (FR-012, DEC-005/DEC-010) |

## No hacer en V1
IA, Finanzas, archivos, marketplace, afiliados, anuncios, grupos/hogares/equipos, roles granulares entre colaboradores, microservicios, Kubernetes, Kafka, Redis salvo necesidad demostrada.
