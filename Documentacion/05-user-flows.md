# 05 — Flujos de usuario

## Flujo principal
```mermaid
flowchart TD
    A[App] --> B{Sesión válida?}
    B -- No --> C[Login]
    C --> D[Autenticación]
    D --> E[Home]
    B -- Sí --> E
    E --> F[Ver pendientes]
    F --> G[Nuevo recordatorio]
    G --> H[Validar]
    H --> I[Guardar]
    I --> E
    F --> J[Completar]
    J --> E
```

## Flujo de error de red
```mermaid
flowchart TD
    A[Acción] --> B[API]
    B --> C{Respuesta}
    C -- Éxito --> D[Actualizar UI]
    C -- Error temporal --> E[Mostrar estado recuperable]
    C -- 401 --> F[Reautenticar]
    C -- 403 --> G[Mostrar sin permiso]
    C -- 4xx --> H[Mostrar error de validación]
    C -- 5xx --> I[Error genérico + retry]
```

## Flujo de compartir recordatorio (invitación) — ADR-006
```mermaid
flowchart TD
    A[Propietario abre recordatorio] --> B[Selecciona Compartir]
    B --> C[Introduce email o username]
    C --> D[Backend valida ownership]
    D --> E[Crea invitación PENDING + expiración 7 días]
    E --> F{Destinatario tiene cuenta?}
    F -- Sí --> G[Push: invitación recibida]
    F -- No --> H[Invitación queda asociada al email]
    G --> I{Destinatario responde o propietario cancela}
    I -- Acepta --> J[Compartición ACTIVA: colaborador ver+completar]
    I -- Rechaza --> K[Invitación REJECTED, sin acceso]
    I -- No responde 7 días --> L[Invitación EXPIRED, sin acceso]
    I -- Propietario cancela (DEC-003) --> P[Invitación CANCELLED, sin acceso]
    J --> M[Push: propietario notificado]
    K --> M
    P --> M
    M --> N[Propietario puede revocar acceso ACTIVE en cualquier momento]
    N --> O[REMINDER_SHARE.status = REVOKED, colaborador pierde acceso de inmediato]
```
Nota: `CANCELLED` (invitación pendiente cancelada por el propietario, UC-14/AC-017) y `REVOKED` (colaboración ya aceptada revocada, UC-10/AC-010) son estados en entidades distintas — `INVITATION` y `REMINDER_SHARE` respectivamente — y nunca se mezclan (DEC-003).

## Flujo de notificaciones push — ADR-007/DEC-010
```mermaid
flowchart TD
    A[Evento de dominio] --> B{Tipo}
    B -- Recordatorio programado --> C[Notificación local en dispositivo]
    B -- Invitación recibida/aceptada/rechazada/cancelada --> D[Push vía PushNotificationSender]
    B -- Cambio en recordatorio compartido --> D
    B -- Revocación de acceso REMINDER_SHARE --> D
    B -- Eliminación de recordatorio compartido --> D
    D --> E[Adapter único: Firebase Cloud Messaging - FCM, DEC-010]
    E --> F[Android nativo]
    E --> G[iOS vía puente FCM-APNs]
    E --> H[Web vía Web Push]
```
