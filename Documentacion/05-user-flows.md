# 05 — Flujos de usuario

**Nota (ADR-015, 2026-08-18):** el destino post-login/post-registro del "Flujo principal" de abajo (paso `E[Home]`) queda superado específicamente en ese punto: desde ADR-015, la vista por defecto al abrir la app es **Calendario**, no Home. El diagrama original se conserva sin reescribir como registro histórico; ver el nuevo flujo "Registro con selección de propósito y navegación por modo" más abajo para el comportamiento vigente.

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

## Flujo de registro con selección de propósito y navegación por modo — ADR-015
```mermaid
flowchart TD
    A[Registro exitoso en Keycloak] --> B["¿Con qué fin usarás la app?"]
    B --> C{Casillas marcadas}
    C -- Personal --> D[personal_enabled = true]
    C -- Laboral --> E[laboral_enabled = true]
    C -- Ambas --> F[personal_enabled = laboral_enabled = true]
    C -- Ninguna --> G[Ambos false, TBD ver ADR-015]
    D --> H[App abre en Calendario general]
    E --> H
    F --> H
    G --> H
    H --> I[Selector superior: Calendario + solo modos habilitados]
    I --> J{Usuario activa modo faltante en Ajustes, FR-016}
    J -- Sí --> K[Modo aparece en el selector]
    J -- No --> I
```

## Flujo de crear tarea vinculada (Módulo Laboral) — ADR-016, UC-17
```mermaid
flowchart TD
    A[Tareas] --> B[Nueva tarea]
    B --> C[Título + fecha]
    C --> D{Asociar?}
    D -- Persona --> E[Selecciona Persona existente]
    D -- Proyecto --> F[Selecciona Proyecto existente]
    D -- Ninguno --> G[Guardar]
    E --> G
    F --> G
    G --> H[REMINDER context=LABORAL persistido]
    H --> I[Visible en Hoy / Agenda]
    H --> J{Tiene Proyecto?}
    J -- Sí --> K[Visible en pestaña Tareas del Proyecto]
    J -- No --> I
```

## Flujo de seguimiento desde una Persona (Módulo Laboral) — ADR-016, UC-18
```mermaid
flowchart TD
    A[Detalle de Persona] --> B[Crear seguimiento]
    B --> C[Describir próxima acción]
    C --> D{Quién debe actuar?}
    D -- Yo --> E[direction = MINE]
    D -- La otra persona --> F[direction = THEIRS]
    E --> G[Fecha + Proyecto opcional]
    F --> G
    G --> H[COMMITMENT persistido]
    H --> I[Visible en Seguimientos → pestaña correspondiente]
    H --> J[Visible en el detalle de la Persona]
```

## Flujo de "Esperando" — resolver o reprogramar (Módulo Laboral) — ADR-016, UC-20
```mermaid
flowchart TD
    A[Seguimientos] --> B[Pestaña Esperando]
    B --> C[Selecciona compromiso THEIRS]
    C --> D[Ver contexto de origen: reunión/nota]
    D --> E{Ya se resolvió?}
    E -- Sí --> F[Marcar resuelto: status = DONE]
    E -- No, sigue pendiente --> G[Reprogramar: nueva due_at]
    F --> H[Sale de la lista de pendientes]
    G --> B
```

## Flujo de reunión → tareas y compromisos (Módulo Laboral) — ADR-016, UC-21
```mermaid
flowchart TD
    A[Agenda] --> B[Abrir reunión: REMINDER con location/participantes]
    B --> C[Ver nota asociada]
    C --> D{Acción}
    D -- Crear tarea --> E[Nuevo REMINDER, mismo Proyecto/Persona]
    D -- Crear seguimiento --> F[Nuevo COMMITMENT, mismo Proyecto/Persona]
    E --> G[Visible en Tareas / Proyecto]
    F --> H[Visible en Seguimientos]
```

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
