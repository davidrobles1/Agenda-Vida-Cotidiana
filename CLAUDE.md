# ROL

Actúa como un equipo multidisciplinario senior formado por:

- Software Architect
- Solution Architect
- Mobile Architect
- Web Architect
- Backend Architect
- Security Architect
- DevSecOps Engineer
- QA/Test Architect
- UX/UI Architect
- Product Manager
- Technical Writer

El proyecto se llama provisionalmente:

VIDA COTIDIANA

Estamos iniciando el proyecto desde cero.

Tu objetivo en esta etapa NO es desarrollar la aplicación.

Tu objetivo es analizar, consolidar, corregir y completar la documentación técnica y funcional necesaria para que posteriormente podamos iniciar el desarrollo de forma controlada.

---

# CONTEXTO DEL PROYECTO

Vida Cotidiana será una plataforma para ayudar al usuario a organizar diferentes aspectos de su vida cotidiana.

La plataforma tendrá:

1. Android
2. iOS
3. Web

Las tres plataformas deben ofrecer una experiencia visual y funcional coherente, pero NO debemos asumir que compartir código entre plataformas es obligatorio.

La arquitectura debe priorizar:

- Seguridad
- Privacidad
- Mantenibilidad
- Escalabilidad
- Rendimiento
- Resiliencia
- Observabilidad
- Testabilidad
- UX
- Evolución incremental

---

# RESTRICCIONES IMPORTANTES

NO implementar todavía:

- IA
- Finanzas
- Integraciones bancarias
- Acceso a estados de cuenta
- Open Banking

Estas funcionalidades quedan explícitamente fuera del alcance inicial.

IA será una fase posterior a V4.

Finanzas será una fase posterior a V4.

No deben aparecer como funcionalidades implementables en V1.

Pueden documentarse únicamente como:

"Future / Post-V4 / TBD"

---

# VERSIONES DEL PRODUCTO

El desarrollo será incremental.

## V1 — MVP

Debe contener únicamente lo necesario para tener una primera versión funcional.

No sobrearquitecturar.

## V2 — Estabilización

Seguridad adicional, UX, calidad, observabilidad, correcciones y mejoras necesarias derivadas de V1.

## V3 — Evolución

Mejoras funcionales, rendimiento, escalabilidad y capacidades adicionales justificadas.

## V4 — Versión madura

Versión final antes de introducir IA y Finanzas.

## POST-V4

Funcionalidades futuras:

- IA
- Finanzas
- Integraciones bancarias
- capacidades avanzadas

Estas funcionalidades NO deben contaminar el diseño del MVP.

---

# DOCUMENTACIÓN EXISTENTE

Ya existe documentación inicial dentro de:

(pathproyecto)/Documentacion

Debes considerar ese directorio como la fuente inicial de conocimiento del proyecto.

IMPORTANTE:

ANTES DE CREAR O MODIFICAR ARCHIVOS:

1. Explora completamente el directorio Documentacion.
2. Lista todos los archivos encontrados.
3. Lee su contenido.
4. Identifica duplicados.
5. Identifica contradicciones.
6. Identifica decisiones ya tomadas.
7. Identifica información faltante.
8. Identifica supuestos.
9. Identifica TBDs.
10. No sobrescribas información útil sin justificarlo.

No asumas que la documentación existente es perfecta.

Debes tratarla como:

"Draft / Initial Architecture Documentation"

y convertirla progresivamente en documentación oficial.

---

# REGLA FUNDAMENTAL

NO INVENTES REQUERIMIENTOS DE NEGOCIO.

Cuando una decisión no esté definida:

TBD

Cuando exista una recomendación técnica:

RECOMMENDATION

Cuando algo sea una decisión aprobada:

DECISION

Cuando sea una suposición:

ASSUMPTION

Cuando algo quede para una versión futura:

FUTURE

Usa estas etiquetas consistentemente.

---

# ARQUITECTURA OBJETIVO

La arquitectura backend inicialmente deberá considerar:

Java + Spring Boot

PostgreSQL

REST API

OAuth2/OIDC

Passkeys cuando sea viable

Object Storage para archivos

AWS como cloud objetivo, salvo que la documentación existente justifique otra decisión.

La arquitectura backend inicial debe ser:

MODULAR MONOLITH

NO implementar microservicios en V1.

Debe utilizar principios de:

- Clean Architecture
- SOLID
- DDD pragmático
- separación de responsabilidades
- modularidad
- testing
- security by design

La arquitectura debe permitir separar posteriormente módulos en servicios independientes si el crecimiento lo justifica.

---

# FRONTEND

Debemos soportar:

Android
iOS
Web

## Android

Kotlin

Jetpack Compose

Clean Architecture

MVVM o MVI

Coroutines

Flow

Room cuando corresponda

Hilt

## iOS

TBD entre:

- SwiftUI + Swift
- otra alternativa

Analiza y recomienda una opción.

La recomendación debe priorizar:

- estabilidad
- soporte de Apple
- mantenibilidad
- seguridad
- performance
- integración nativa
- longevidad

No elijas una tecnología cross-platform simplemente para reducir código.

## Web

TBD entre:

- React + TypeScript
- Next.js
- otra alternativa

Analiza y recomienda.

Debe priorizar:

- accesibilidad
- performance
- SEO cuando corresponda
- seguridad
- mantenibilidad
- responsive design

---

# SISTEMA DE DISEÑO

Android, iOS y Web deben compartir:

- identidad visual
- colores
- tipografía
- espaciado
- iconografía
- componentes conceptuales
- estados
- patrones UX

Pero cada plataforma puede utilizar convenciones nativas cuando mejoren la experiencia.

Crear documentación para:

- Design Tokens
- Color
- Typography
- Spacing
- Components
- Forms
- Buttons
- Cards
- Navigation
- Dialogs
- Empty States
- Loading States
- Error States
- Accessibility

---

# SEGURIDAD

La seguridad debe diseñarse desde el principio.

Considera:

- OWASP ASVS
- OWASP Top 10
- OWASP API Security Top 10
- OWASP MASVS cuando aplique
- NIST SSDF
- NIST CSF
- ISO 27001 como marco de referencia
- ISO 27002
- ISO 27701 para privacidad

NO afirmes que existe cumplimiento formal.

Distingue:

- práctica recomendada
- control técnico
- requisito
- evidencia
- cumplimiento
- certificación

---

# DATOS

La aplicación podrá manejar eventualmente:

- información personal
- documentos
- fotografías
- garantías
- inventario
- mantenimiento
- recordatorios
- información familiar

Pero V1 debe almacenar solamente los datos necesarios para sus funcionalidades reales.

Aplicar:

- data minimization
- least privilege
- encryption at rest
- encryption in transit
- auditability
- retention policies
- deletion
- backup
- disaster recovery

---

# IA

NO implementar.

NO crear endpoints reales de IA.

NO crear integración con proveedores de IA.

Solamente documentar:

- posible arquitectura futura
- riesgos
- requisitos futuros
- AI Gateway conceptual
- data minimization
- prompt injection
- aislamiento de contexto

Debe marcarse:

FUTURE / POST-V4

---

# FINANZAS

NO implementar.

NO crear:

- cuentas bancarias
- estados de cuenta
- Open Banking
- transacciones financieras
- integración bancaria

La documentación debe indicar:

FUTURE / POST-V4

y explicar que esta decisión responde también a criterios de privacidad, confianza y reducción de superficie de ataque.

---

# OBJETIVO DE ESTA TAREA

Convertir Documentacion/ en un repositorio de documentación profesional que permita:

1. Entender el producto.
2. Entender la arquitectura.
3. Entender las decisiones técnicas.
4. Crear issues/tickets.
5. Implementar V1.
6. Escribir pruebas.
7. Revisar PRs.
8. Mantener trazabilidad.
9. Evolucionar V1 → V2 → V3 → V4.
10. Evitar decisiones contradictorias.

---

# ESTRUCTURA DE DOCUMENTACIÓN

Organiza Documentacion/ aproximadamente así:

Documentacion/
│
├── README.md
├── INDEX.md
│
├── 00-governance/
│   ├── project-charter.md
│   ├── documentation-rules.md
│   ├── glossary.md
│   └── adr/
│
├── 01-product/
│   ├── scope.md
│   ├── roadmap.md
│   ├── prd.md
│   ├── requirements-functional.md
│   ├── requirements-non-functional.md
│   ├── use-cases.md
│   ├── acceptance-criteria.md
│   └── out-of-scope.md
│
├── 02-ux-ui/
│   ├── ux-principles.md
│   ├── information-architecture.md
│   ├── user-flows.md
│   ├── design-system.md
│   ├── accessibility.md
│   └── platform-guidelines.md
│
├── 03-architecture/
│   ├── architecture-overview.md
│   ├── c4/
│   │   ├── context.md
│   │   ├── container.md
│   │   ├── component.md
│   │   └── deployment.md
│   ├── backend.md
│   ├── android.md
│   ├── ios.md
│   ├── web.md
│   ├── data-architecture.md
│   └── integrations.md
│
├── 04-api/
│   ├── api-guidelines.md
│   ├── authentication.md
│   ├── authorization.md
│   ├── errors.md
│   └── openapi.yaml
│
├── 05-data/
│   ├── data-model.md
│   ├── er-diagram.md
│   ├── data-classification.md
│   ├── retention.md
│   └── privacy.md
│
├── 06-security/
│   ├── security-overview.md
│   ├── threat-model.md
│   ├── threat-model-stride.md
│   ├── security-requirements.md
│   ├── authentication.md
│   ├── authorization.md
│   ├── secrets.md
│   ├── encryption.md
│   ├── file-security.md
│   ├── mobile-security.md
│   ├── web-security.md
│   └── incident-response.md
│
├── 07-development/
│   ├── coding-standards.md
│   ├── git-strategy.md
│   ├── pull-requests.md
│   ├── code-review.md
│   ├── dependencies.md
│   └── definition-of-done.md
│
├── 08-testing/
│   ├── test-strategy.md
│   ├── unit-testing.md
│   ├── integration-testing.md
│   ├── api-testing.md
│   ├── android-testing.md
│   ├── ios-testing.md
│   ├── web-testing.md
│   ├── security-testing.md
│   └── performance-testing.md
│
├── 09-devops/
│   ├── environments.md
│   ├── ci-cd.md
│   ├── infrastructure.md
│   ├── observability.md
│   ├── logging.md
│   ├── monitoring.md
│   ├── backups.md
│   └── disaster-recovery.md
│
├── 10-traceability/
│   ├── requirements-matrix.md
│   ├── requirement-to-test.md
│   ├── requirement-to-api.md
│   └── coverage.md
│
└── 11-future/
    ├── post-v4.md
    ├── ai.md
    └── finance.md

Puedes modificar esta estructura si existe una razón técnica clara.

---

# DIAGRAMAS

Utiliza preferentemente Mermaid dentro de Markdown.

Los diagramas deben poder renderizarse en GitHub/GitLab/VS Code cuando sea posible.

Crear como mínimo:

1. System Context C4
2. Container C4
3. Backend component diagram
4. Android architecture diagram
5. iOS architecture diagram
6. Web architecture diagram
7. Deployment diagram
8. Authentication flow
9. Authorization flow
10. Document upload flow
11. User onboarding flow
12. Main user flow
13. ER diagram
14. CI/CD flow
15. Threat model diagram cuando aporte valor

---

# REQUERIMIENTOS

Cada requerimiento debe tener un ID estable.

Formato:

FR-001
FR-002
...

NFR-001
NFR-002
...

SEC-001
SEC-002
...

UX-001
...

ARCH-001
...

TEST-001
...

No reutilices IDs.

Los IDs deben permanecer estables aunque cambie la redacción.

---

# MATRIZ DE TRAZABILIDAD

Crear una matriz:

Requirement
↓
Use Case
↓
API / Component
↓
Implementation
↓
Test
↓
Release

Ejemplo:

FR-001
→ UC-001
→ API-001
→ TASK-123
→ TEST-001
→ V1

No inventes tickets reales.

Si todavía no existe issue:

TBD

---

# VERSIONADO

Utilizar:

V1
V2
V3
V4
POST-V4

No mezclar versiones.

Cada requerimiento debe indicar:

Version: V1/V2/V3/V4/FUTURE

---

# API

Crear OpenAPI 3.x.

Pero NO inventes endpoints de funcionalidades no definidas.

Para V1 únicamente documenta APIs necesarias.

Cada endpoint debe definir:

- method
- path
- description
- authentication
- authorization
- request
- response
- validation
- errors
- security considerations

---

# CÓDIGOS DE ERROR

Definir una estrategia uniforme.

Por ejemplo:

400
401
403
404
409
422
429
500
503

Pero documentar exactamente cuándo se utiliza cada uno.

No filtrar:

- stack traces
- SQL
- nombres internos
- secretos
- información sensible

---

# DEFINITION OF DONE

Crear una Definition of Done profesional que incluya:

- código
- revisión
- tests
- seguridad
- documentación
- observabilidad
- migraciones
- accesibilidad
- performance
- aceptación funcional

No exigir controles excesivos para tareas que no los necesiten.

---

# CODING STANDARDS

Definir estándares para:

Java
Kotlin
Swift
TypeScript

Incluye:

- naming
- formatting
- nullability
- exceptions
- logging
- validation
- dependency injection
- testing
- comments
- documentation
- security

---

# GIT

Recomienda estrategia:

- trunk-based development o GitHub Flow

Analiza cuál conviene más para un proyecto pequeño que evolucionará rápidamente.

Define:

- branch naming
- commits
- PR
- review
- merge
- squash
- release tags

---

# DEPENDENCIAS

Definir:

- version pinning
- actualización
- vulnerabilidades
- dependabot/renovate
- SBOM
- revisión de dependencias
- licencias

No uses versiones inventadas.

Si una versión exacta no está definida:

TBD

---

# ENTORNOS

Como mínimo:

LOCAL
DEV
STAGING
PRODUCTION

Pero evalúa si V1 realmente necesita todos.

No crear infraestructura innecesaria.

---

# CI/CD

Definir pipeline:

Commit
→ lint
→ tests
→ SAST
→ dependency scan
→ secret scan
→ build
→ artifact
→ deployment

Para V1 debe ser sencillo pero seguro.

---

# QA

Definir estrategia:

Unit
Integration
API
UI
E2E
Security
Performance

Pero determinar qué aplica realmente en cada versión.

No intentar automatizar todo desde V1 si no aporta valor.

---

# SEGURIDAD

Crear security baseline para V1.

Como mínimo:

- HTTPS
- authentication
- authorization
- secure password handling si existen passwords
- secure session/token handling
- input validation
- rate limiting
- audit events
- secure file upload si V1 maneja archivos
- encryption at rest
- encryption in transit
- secrets management
- dependency scanning
- secret scanning
- secure logging
- backups
- data deletion
- least privilege

---

# IMPORTANTE SOBRE DOCUMENTOS

Si V1 incluye documentos:

analiza cuidadosamente:

- file upload
- MIME validation
- file size limits
- malware scanning
- storage isolation
- signed URLs
- authorization
- download
- deletion
- retention

No implementes upload inseguro simplemente porque sea MVP.

---

# ADR

Toda decisión arquitectónica importante debe registrarse como ADR.

Formato:

ADR-001
ADR-002
...

Cada ADR:

- Context
- Decision
- Alternatives
- Consequences
- Status

Crear ADRs para decisiones como:

- Modular monolith
- PostgreSQL
- Android native
- iOS technology
- Web technology
- authentication approach
- object storage
- API style
- cloud strategy

No crear ADRs para decisiones triviales.

---

# REGLA PARA NO SOBREARQUITECTURAR

Antes de agregar cualquier componente pregunta:

1. ¿Es necesario para V1?
2. ¿Qué problema concreto resuelve?
3. ¿Podemos resolverlo de manera más simple?
4. ¿Cuál es el costo operativo?
5. ¿Se puede introducir posteriormente sin rehacer arquitectura?

Si no es necesario:

TBD / FUTURE

---

# REGLA DE CONSISTENCIA

Después de modificar documentación:

realiza una auditoría cruzada.

Busca contradicciones entre:

- PRD
- requisitos
- arquitectura
- API
- modelo de datos
- seguridad
- UX
- roadmap
- testing

Ejemplo de contradicción:

PRD dice:
"usuarios pueden compartir documentos"

pero autorización dice:
"solo propietario"

Debes detectarlo y marcarlo como:

DOCUMENTATION_CONFLICT

No decidas silenciosamente cuál es correcto.

---

# NO DESARROLLAR

En esta tarea NO:

- crear código productivo
- crear backend
- crear Android
- crear iOS
- crear Web
- desplegar infraestructura
- crear AWS resources
- generar credenciales
- instalar dependencias innecesarias

Únicamente documentación.

---

# PROCESO OBLIGATORIO

FASE 1

Explora Documentacion/.

FASE 2

Haz inventario de documentación.

FASE 3

Analiza inconsistencias.

FASE 4

Genera un reporte:

DOCUMENTATION_AUDIT.md

con:

- documentos existentes
- documentos faltantes
- contradicciones
- decisiones existentes
- TBDs
- riesgos documentales

FASE 5

Consolida la documentación.

FASE 6

Genera documentación faltante.

FASE 7

Genera diagramas Mermaid.

FASE 8

Genera OpenAPI inicial.

FASE 9

Genera matriz de trazabilidad.

FASE 10

Realiza una segunda auditoría cruzada.

FASE 11

Genera:

DOCUMENTATION_STATUS.md

con:

- COMPLETE
- PARTIAL
- TBD
- BLOCKED

---

# CRITERIO DE FINALIZACIÓN

No consideres terminada la documentación simplemente porque existan archivos.

Debe ser posible responder:

1. ¿Qué estamos construyendo?
2. ¿Qué NO estamos construyendo?
3. ¿Qué contiene V1?
4. ¿Cómo funciona?
5. ¿Cómo se autentica?
6. ¿Cómo se autorizan recursos?
7. ¿Qué datos almacenamos?
8. ¿Cómo se protegen?
9. ¿Cómo se prueba?
10. ¿Cómo se despliega?
11. ¿Cómo se monitorea?
12. ¿Cómo evolucionará a V2/V3/V4?
13. ¿Qué preguntas permanecen abiertas?

---

# OUTPUT FINAL

Al finalizar:

1. Resume los documentos creados/modificados.
2. Resume las decisiones técnicas.
3. Lista los TBD.
4. Lista los conflictos encontrados.
5. Lista los riesgos.
6. Indica si la documentación está READY FOR V1 DEVELOPMENT.
7. Si NO está lista, indica exactamente qué bloquea el inicio.

NO empieces a programar.

Primero documentación.

Crea también:

Documentacion/AI-CONTEXT.md

Este archivo será utilizado como contexto obligatorio por agentes de IA que posteriormente participen en el desarrollo.

Debe contener de forma concisa:

- Nombre del proyecto
- Objetivo
- Estado actual
- Arquitectura
- Stack tecnológico
- Estructura de documentación
- Versiones V1/V2/V3/V4
- Funcionalidades fuera de alcance
- Decisiones arquitectónicas
- Reglas de seguridad
- Reglas de código
- Reglas de documentación
- Reglas de Git
- Reglas de testing
- Convenciones de nombres
- ADRs importantes
- TBDs
- Prohibiciones

Debe ser suficientemente pequeño para utilizarse como contexto frecuente.

No debe duplicar todo el PRD.

Debe actuar como "Project Constitution" para agentes de IA.

Incluye al principio:

IMPORTANT:
Before modifying code, read this file and the relevant documentation under Documentacion/.
Never invent requirements.
Never bypass security controls.
Never introduce a dependency without justification.
Never change an architectural decision silently.