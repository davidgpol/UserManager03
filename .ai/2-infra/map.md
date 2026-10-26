# UserManager03 — Repository Map

Priority: 3

## Purpose

Mapa con inspección histórica de HAR-83, run 6ccc3a9d-552a-4a71-8943-e7232274a8c1, 2026-10-26T09:31:59.414823+00:00; se conserva como fotografía de aquella fecha. Identidad en `.ai/0-core/project.xml`; no demuestra build, despliegue ni aceptación.

## Structure status

Existente = observado en destino en la inspección indicada; planificado = ubicación seleccionada no creada en esa inspección; generado = salida futura del build. Ninguna observación histórica establece por sí sola el estado posterior.

Estado documental: paquete de promoción S02-NIOBE-3P preparado en HAR-101 para confirmación exacta de Neo en HAR-80; todavía no confirmado, instalado ni publicado. Procede de S02-NIOBE-3C (HAR-99, revisión 683a3807-a17c-42f8-a008-3c4b2e04d891), con PASS documental de Seraph en HAR-100 (revisión 91ce925c-987e-4fc4-879e-0d420371e7e1). Los tres XML presentan status=approved como estado preparado para la promoción autorizada en HAR-101; este atributo no sustituye la confirmación del nuevo manifiesto. general.xml y security.xml conservan los bytes aprobados de S02-NIOBE-2P, sin nuevas reglas. Conforme a .ai/1-rules/general.xml AUTH_02/AUTH_03, Neo debe confirmar los siete hashes antes de asignar instalación y comparación/checker final en destino. Al preparar este paquete, la gobernanza instalada/publicada sigue siendo S02-NIOBE-2P. Esta entrega no acredita preparación inicial, aceptación ni PROJECT READY.

Inspección histórica del destino durante HAR-89: 2026-10-26T10:00:36.566553+00:00, HAR-89, run 340d756d-5d71-4c5f-ae57-de7b6b77d5ba. Solo se observó `.git`; raíz exacta `/opt/Dev/Projects/UserManager03`, `main` sin commits y gobernanza ausente. Es evidencia fechada anterior a cualquier instalación. Los tres comandos Git de solo lectura terminaron 0. No se modifica el destino.

## Repository structure — inspección histórica de HAR-83

```text
/opt/Dev/Projects/UserManager03/ [existente; Git raíz exacta, main sin commits]
  .git/                        [único contenido existente observado]
  AGENTS.md                    [borrador S02, no instalado]
  .ai/                         [seis archivos de borrador S02, no instalados]
  api/openapi.yaml             [planificado, antes de endpoints]
  backend/pom.xml              [planificado, implementa project.xml]
  backend/src/main/java/com/hariklabs/usermanager03/ [planificado]
    rest/ service/ persistence/ security/ config/  [planificados]
  backend/src/main/resources/db/migration/         [planificado]
  backend/src/test/             [planificado]
  frontend/package.json        [planificado, implementa project.xml]
  frontend/src/                [planificado]
  frontend/src/generated/api/  [generado por build futuro]
  platform/                    [planificado: infraestructura y scripts]
```

En aquella inspección no se inspeccionaron ni reutilizaron otras aplicaciones. Cwd real de aquel run: `/opt/paperclip/instances/default/workspaces/fe140296-b893-4ced-bd18-6986e242ff09`. projectId, projectWorkspaceId y executionWorkspaceId nulos. Este cwd no es el destino autorizado del proyecto.

## Inspección actual HAR-99 — 2026-10-26T12:20:49.428804+00:00

Run `6308ec45-91c9-441e-936b-6a858f229dd6`, Niobe. Cwd real y Git top-level `/opt/Dev/Projects/UserManager03`; workspace `dd624f4d-37ba-4938-994a-d600f8b0bd98`, main. Gobernanza instalada: siete archivos S02-NIOBE-2P leídos y hashes coincidentes. `.git`, AGENTS.md y seis archivos .ai existen; api/, backend/, frontend/, platform/ y README.md no existen, siguen planificados. No hay aplicación/descriptores/contrato/despliegue verificados.

Repositorio creado público y publicado desde este runtime: https://github.com/davidgpol/UserManager03.git, main local/remoto `3b5c7debdd9c6c75211f5bab5a70fa0ae359cef9`; árbol limpio. Solo siete archivos confirmados, sin este candidato. Identidad gh `davidgpol`; primer push exit 128 por helper ausente; push con helper gh limitado al comando exit 0. Sin cambios globales. Evidencia HAR-99, work product commit. Neo actualiza repoUrl del workspace; la configuración observada inicialmente tenía repoUrl nulo.

Checker real de project-bootstrap 2.2.0, --workspace cwd --expected-workspace ruta autorizada --stage final: exit 0 para paquete instalado. Runner Linux aarch64, Git 2.43.0, Python 3.12.3, Node 24.21.0 y npm 11.19.0; Java/Maven/psql no disponibles en PATH. Node/npm difieren de identidad 24.22.1/11.19.1. No se cambian versiones normativas ni se instalan paquetes. Docker CLI 29.8.2 observado solo con --version; daemon, imágenes y servicios no comprobados. Build integrado fuera de alcance. Anexo y capturas HAR-99 conservan comandos y resultados; static PASS no acredita compatibilidad ni aceptación.

## Main flow

Diseño seleccionado; ilustración, no ejecución:

```text
David → SSH existente + HTTPS validado → React + Spring Boot
React → REST → Servicio → Persistencia JPA → PostgreSQL
         ↕         ↕
   mapper manual   transacciones
```

## Component responsibilities

| Componente | Ubicación | Responsabilidad | Estado |
|---|---|---|---|
| Contrato | api/openapi.yaml | Esquemas y cinco operaciones CRUD | Planificado |
| REST | backend/.../rest | HTTP, validación, mapper manual | Planificado |
| Servicio | backend/.../service | Negocio, transacciones, resultados propios | Planificado |
| Persistencia | backend/.../persistence | Entidades JPA y repositorios | Planificado |
| Autenticación | backend/.../security | Sesión de operador separada del CRUD | Planificado |
| UI | frontend/src | React Material Design responsive | Planificado |
| Infraestructura | platform/ | Dockerfiles, Compose, configuración y scripts operativos | Planificado |

## Build, configuration and storage

| Elemento | Ubicación | Rol | Estado |
|---|---|---|---|
| Maven/npm | backend/pom.xml; frontend/package.json | Reflejar identidad aprobada | Planificados |
| Java generado | backend/target/generated-sources/openapi | Interfaces/DTO | Generado futuro |
| PostgreSQL | volumen usermanager03-postgres-data → /var/lib/postgresql | Datos permanentes protegidos | Diseñado, no creado/verificado |
| Secretos/certificados | Origen privado fuera de Git → /run/secrets | Configuración de ejecución | Rutas host/trust pendientes de inspección |
| Red | red privada del Compose usermanager03 | BD interna, TLS verify-full | Diseñada, no creada |
| HTTPS | 127.0.0.1:18443 → 8443 | Acceso privado por túnel SSH | Puerto objetivo, no comprobado libre |

## Referenced artifacts

| Artefacto | Estado | Relación |
|---|---|---|
| https://github.com/davidgpol/UserManager03.git público | Público, main publicado en HAR-99; ver inspección actual | HAR-79/HAR-99; S04 ejecutado |
| Despliegue Compose usermanager03 | Planificado | Niobe bajo encargo operativo de Neo |
| Portainer existente | Visibilidad no comprobada | Evidencia final requerida, sin reconfiguración global |
| Informe final con capturas por caso | Pendiente implementación/QA | HAR-79 y matriz HAR-82 |
| Gobernanza S02-NIOBE-1 y SHA-256 | Artefacto histórico HAR-83; FAIL documental HAR-88 | Se conserva íntegro como antecedente, no instalar |
| Gobernanza corregida y SHA-256 | Manifiesto exacto HAR-89; estado documental indicado arriba | HAR-90 reverifica; HAR-91 coordina confirmación e instalación |

## Dependencias y límites

Estado vigente de decisiones: Q-F01–Q-F04=A autenticadas en HAR-85 y concretadas por Trinity en HAR-84 revisión 97c325a4-4516-463b-bb07-500a0bcb80ac; PD-05–10 decididas por Niobe en HAR-86 revisión e8e12e4e-90c2-4657-bc58-9a3f1095f9c1; preparación integrada HAR-96 revisión cd64f9ae-11bf-424d-9272-f3f9b937f288, Unicode 16.0.0. Se incorporan a la identidad del candidato; no queda pregunta humana funcional pendiente. Se conservan 56 RE/CD y 4 COND hasta las puertas de publicación de contrato/fixtures del expediente. OpenAPI todavía no existe; Neo coordina su encargo posterior a la preparación inicial. Las reservas antiguas de HAR-86 solo ceden en lo resuelto por las fuentes Q-F.

Antecedente: el mapa S02-NIOBE-2P registró PD-01–09 como pendientes funcionales y PD-10 como concreción de HAR-83. Ese estado histórico ha quedado superado por las decisiones anteriores; el archivo íntegro anterior se conserva en el commit inicial 3b5c7debdd9c6c75211f5bab5a70fa0ae359cef9 y en el diff del candidato HAR-99.

Niobe comprobará en fase operativa: daemon/permisos, manifests/digests ARM64, puerto disponible, alias SSH autorizado, forwarding/host key, certificado SAN/confianza de David, rutas privadas y entrega de secretos, recursos y Portainer. Si requiere cambios globales, Neo encauza acción fuera de esta autorización. La etiqueta Temurin 25-jdk-noble observada en S01 tenía 25.0.4.1_1 frente al JDK seleccionado 25.0.5+8; conservar selección y usar distribución ARM64 exacta con checksum o imagen exacta comprobada. No hay build integrado ni medición Argon2. URL https://localhost:18443 es objetivo condicionado al túnel/certificado, no URL entregada u operativa.

## Map maintenance

Al promover, sustituir el bloque de estado documental junto con AGENTS.md y los tres XML; confirmar los hashes resultantes antes de instalar. Conservar la inspección histórica fechada. Una instalación comprobada se documentará como nueva inspección fechada, con rutas y evidencia, sin reescribir el pasado. Actualizar mapa cuando cambien ubicaciones; registrar ejecución real en `.ai/3-runtime/progress.log`. No interpretar planificado como existente.

## Inspección y trabajo HAR-107 — 2026-10-26T15:40:46.916563+00:00

Run `7e663ce2-43fc-488e-a1ee-d1a44950d594`, Niobe. Main inicial limpia en
`1ab754f9582f1731d4684370036543fc48ccf2ab`; preparación confirmada por S07 de HAR-80
revisión `07cb1d8c-c921-46de-8a4a-6e2bcf1b601c`. Las referencias anteriores de
promoción pendiente son fotografías históricas, no puertas reabiertas.

Existen ahora `README.md`, `api/openapi.yaml`, `api/fixtures/`, `api/unicode/`,
`platform/toolchain/` y `platform/openapi/`. Contrato validado y generación de
sondeo en scratch, sin endpoints ni aplicación. Publicación pendiente de tarjeta
GitHub de HAR-107. No existen todavía backend/, frontend/, Compose, volumen
permanente ni servicio de producto creados por esta entrega.

Toolchain exacto construido ARM64. Configuraciones de generación y probes de
correo/DTO conservados en platform; Java y TypeScript de sondeo no son componentes
instalados. SHA del contrato, comandos, versiones, capturas y recursos en HAR-107.
La imagen propia de toolchain se retiene para continuar; no es despliegue final.

Incidente: Buildx docker-container creó auxiliar privilegiado, contrario a
security.xml OPS_02. Constructor propio y volumen retirados; cambio a driver
Docker existente sin auxiliar. Evidencia y corrección en HAR-107, comunicada a
Neo en HAR-80. No se modifica la regla ni se afirma conformidad del intento inicial.
