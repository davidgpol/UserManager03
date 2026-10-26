# UserManager03 — Repository Map

Priority: 3

## Purpose

Mapa con inspección histórica de HAR-83, run 6ccc3a9d-552a-4a71-8943-e7232274a8c1, 2026-10-26T09:31:59.414823+00:00; se conserva como fotografía de aquella fecha. Identidad en `.ai/0-core/project.xml`; no demuestra build, despliegue ni aceptación.

## Structure status

Existente = observado en destino en la inspección indicada; planificado = ubicación seleccionada no creada en esa inspección; generado = salida futura del build. Ninguna observación histórica establece por sí sola el estado posterior.

Estado documental de esta revisión: S02-NIOBE-2P, confirmada por Neo mediante el manifiesto exacto de HAR-91 tras dictamen conforme de Seraph en HAR-90; los tres XML tienen `status=approved`. El estado documental no prueba instalación; consultar en HAR-91 la evidencia fechada de instalación, comparación de bytes y checker final.

Última inspección del destino durante esta corrección: 2026-10-26T10:00:36.566553+00:00, HAR-89, run 340d756d-5d71-4c5f-ae57-de7b6b77d5ba. Solo se observó `.git`; raíz exacta `/opt/Dev/Projects/UserManager03`, `main` sin commits y gobernanza ausente. Es evidencia fechada anterior a cualquier instalación. Los tres comandos Git de solo lectura terminaron 0. No se modifica el destino.

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
| https://github.com/davidgpol/UserManager03.git público | Autorizado; no creado ni acceso comprobado por S02 | HAR-79; S04 posterior |
| Despliegue Compose usermanager03 | Planificado | Niobe bajo encargo operativo de Neo |
| Portainer existente | Visibilidad no comprobada | Evidencia final requerida, sin reconfiguración global |
| Informe final con capturas por caso | Pendiente implementación/QA | HAR-79 y matriz HAR-82 |
| Gobernanza S02-NIOBE-1 y SHA-256 | Artefacto histórico HAR-83; FAIL documental HAR-88 | Se conserva íntegro como antecedente, no instalar |
| Gobernanza corregida y SHA-256 | Manifiesto exacto HAR-89; estado documental indicado arriba | HAR-90 reverifica; HAR-91 coordina confirmación e instalación |

## Dependencias y límites

HAR-82 revisión 8c107607-8ee5-4f56-a2ff-ed68731b0771: PD-01 (límites de nombre/apellidos), PD-02 (DNI/normalización), PD-03 (coincidencia por campo), PD-04 (mayúsculas/tildes/espacios), PD-05 (combinación y páginas), PD-06 (baja repetida), PD-07 (IDs/errores de negocio), PD-08 (campos editables/PUT/desconocidos) y PD-09 (orden/desempate) siguen pendientes de decisión funcional. Trinity concreta y Neo encauza decisiones materiales; no se implementará el endpoint afectado sin resolución trazada. Preparación independiente continúa. PD-10 se concreta técnicamente por Niobe en HAR-83: inactividad 30 min configurable, sesiones independientes, logout invalida la sesión usada y reinicio invalida todas; no se prescribe un límite funcional de concurrencia.

Niobe comprobará en fase operativa: daemon/permisos, manifests/digests ARM64, puerto disponible, alias SSH autorizado, forwarding/host key, certificado SAN/confianza de David, rutas privadas y entrega de secretos, recursos y Portainer. Si requiere cambios globales, Neo encauza acción fuera de esta autorización. La etiqueta Temurin 25-jdk-noble observada en S01 tenía 25.0.4.1_1 frente al JDK seleccionado 25.0.5+8; conservar selección y usar distribución ARM64 exacta con checksum o imagen exacta comprobada. No hay build integrado ni medición Argon2. URL https://localhost:18443 es objetivo condicionado al túnel/certificado, no URL entregada u operativa.

## Map maintenance

Al promover, sustituir el bloque de estado documental junto con AGENTS.md y los tres XML; confirmar los hashes resultantes antes de instalar. Conservar la inspección histórica fechada. Una instalación comprobada se documentará como nueva inspección fechada, con rutas y evidencia, sin reescribir el pasado. Actualizar mapa cuando cambien ubicaciones; registrar ejecución real en `.ai/3-runtime/progress.log`. No interpretar planificado como existente.
