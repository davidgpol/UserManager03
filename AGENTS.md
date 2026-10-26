# UserManager03 — Agent Instructions

This file is the single entry point for project-specific agent instructions.

## Project purpose

Web de gestión de fichas de usuarios y autenticación de cuenta operadora separada. Spring Boot REST API-first, React Material Design y PostgreSQL; versiones normativas en project.xml.

Approved versions and architectural decisions belong in
`.ai/0-core/project.xml`.

## Estado documental

Estado documental: paquete de promoción S02-NIOBE-3P preparado en HAR-101 para confirmación exacta de Neo en HAR-80; todavía no confirmado, instalado ni publicado. Procede de S02-NIOBE-3C (HAR-99, revisión 683a3807-a17c-42f8-a008-3c4b2e04d891), con PASS documental de Seraph en HAR-100 (revisión 91ce925c-987e-4fc4-879e-0d420371e7e1). Los tres XML presentan status=approved como estado preparado para la promoción autorizada en HAR-101; este atributo no sustituye la confirmación del nuevo manifiesto. general.xml y security.xml conservan los bytes aprobados de S02-NIOBE-2P, sin nuevas reglas. Conforme a .ai/1-rules/general.xml AUTH_02/AUTH_03, Neo debe confirmar los siete hashes antes de asignar instalación y comparación/checker final en destino. Al preparar este paquete, la gobernanza instalada/publicada sigue siendo S02-NIOBE-2P. Esta entrega no acredita preparación inicial, aceptación ni PROJECT READY.

La inspección histórica y las ubicaciones planificadas se distinguen en `.ai/2-infra/map.md`. Al promover, actualizar este estado y el del mapa junto con los tres XML; confirmar el nuevo manifiesto antes de instalar. No basta cambiar los atributos XML.

## Project context

Read the following documents before work that depends on them:

| Document | Purpose |
| --- | --- |
| `.ai/0-core/project.xml` | Project identity, scope, technology choices and architecture. |
| `.ai/1-rules/security.xml` | Project security requirements. |
| `.ai/1-rules/general.xml` | Development, validation and logging rules. |
| `.ai/2-infra/map.md` | Component locations and relationships. |
| `.ai/3-runtime/progress.log` | Actual execution history and pending next steps. |
| `.ai/3-runtime/lessons_learned.md` | Verified knowledge applicable to the work. |

For execution history, focus on entries relevant to the assigned task.
Missing bootstrap artifacts are pending work; create them when in assigned scope.
Do not treat planned files as existing files.

## Source of project decisions

Implement the approved identity in build files and code.
Do not redefine identity merely to match an implementation discrepancy.
Use map.md to locate components, not to introduce architectural decisions.
History and lessons provide context and do not override approved rules.
Identify both the file and rule identifier when referring to a rule.

## Working within Paperclip

Follow the role, scope and authorization provided through Paperclip.
This repository does not redefine personas, reporting lines or delegation.
Use actual issue and run identifiers; do not invent them.

## Repository organization

Ubicaciones planificadas: backend/src y backend/pom.xml; frontend/src y frontend/package.json; contrato api/openapi.yaml. Capas REST → Servicio → Persistencia → BD; no hexagonal. En la inspección de 2026-10-26T10:00:36.566553+00:00 no había aplicación ni descriptores creados; consultar el mapa y la tarea actual para cambios posteriores. Cargar la tarea actual de Paperclip y sus dependencias antes de actuar; HAR-80 contiene el plan único. Las PR/PD de HAR-82 no son aprobaciones por sí mismas; aplicar las decisiones posteriores Q-F01–Q-F04 y PD-05–10 trazadas en project.xml, HAR-84, HAR-86 y HAR-96. Véase el mapa para dependencias funcionales y límites operativos.

Project infrastructure belongs under `platform/`: Dockerfiles and their
build-context exclusions, Compose definitions, infrastructure configuration,
deployment templates and operational scripts.
Detailed locations and status belong in `.ai/2-infra/map.md`.

## Container operations

Use infrastructure definitions under `platform/`.
Keep the repository root as build context when required to access application
source or build descriptors.
Resolve commands against actual infrastructure files.
Document verified commands in README.md; label unexecuted proposals accordingly.

## Maintaining project context

Record actual actions and outcomes in `.ai/3-runtime/progress.log`.
Update `.ai/2-infra/map.md` when locations or relationships change.
Add verified reusable knowledge to `.ai/3-runtime/lessons_learned.md`.
Keep decisions in project.xml and rules in their corresponding rule files.

## Comandos y estado de preparación

Verificados históricamente en HAR-83 (2026-10-26T09:31:59.414823+00:00): `git -C /opt/Dev/Projects/UserManager03 rev-parse --show-toplevel`, `git -C /opt/Dev/Projects/UserManager03 symbolic-ref --short HEAD`, `git -C /opt/Dev/Projects/UserManager03 status --short --branch` (exit 0; main sin commits). Esa inspección no verificó comandos de build/despliegue. No ejecutar comandos de infraestructura hasta disponer de definiciones reales en platform/ y una tarea asignada.

La revisión de Neo confirma un manifiesto exacto antes de instalar; el checker final y comparación de bytes se ejecutarán después en destino. David ya autorizó publicación pública HTTPS y Docker específicos de este proyecto (HAR-79); HAR-83 no ejecuta esas fases. No hay autorización genérica para otros proyectos ni configuración global. Las decisiones técnicas delegadas no necesitan repetición de aprobación de David.
