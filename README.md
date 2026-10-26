# UserManager03

Gestión de fichas de usuarios y cuenta operadora independiente. Identidad y
versiones normativas en `.ai/0-core/project.xml`; instrucciones en `AGENTS.md`.

Estado de HAR-107: toolchain ARM64 construido/verificado y contrato OpenAPI
validado y publicado en `4b282103f7c4d1f7d9399c184cd4c408b44f9c29`
mediante Git y helper `gh` de OCI (SHA remoto verificado). Backend implementado con 14 pruebas de desarrollo previas; frontend integrado y
pruebas reales de navegador pasan. Servicio HTTPS persistente local operativo.
David confirmó funcionamiento de acceso y Portainer el 2026-10-26; evidencia
declarada por el operador, sin observación autenticada de Niobe en Portainer.
QA independiente y aceptación pendientes.

## Toolchain verificado

`platform/toolchain/Dockerfile` instala dentro de la imagen exclusivamente:
Temurin 25.0.5+8, Maven 3.10.0, Node 24.22.1, npm 11.19.1 y OpenAPI Generator
7.26.0. Distribuciones y checksums oficiales en `distributions.json`. No instala
paquetes en el host. La base Ubuntu se fijó por manifiesto `linux/arm64`. Se utiliza el driver
`docker` existente, sin contenedor auxiliar privilegiado. Las dependencias del SO de la imagen se obtienen de apt,
por lo que no se afirma reproducibilidad binaria de esas capas futuras.

Fuentes oficiales verificadas el 2026-10-26:

- [Temurin 25.0.5+8](https://github.com/adoptium/temurin25-binaries/releases/tag/jdk-25.0.5%2B8).
- [Maven 3.10.0](https://maven.apache.org/download.cgi).
- [Node 24.22.1 y checksums](https://nodejs.org/dist/v24.22.1/SHASUMS256.txt).
- [npm 11.19.1](https://registry.npmjs.org/npm/11.19.1).
- [BOM Spring Boot 4.1.2](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom).

Comandos ejecutados desde la raíz del repositorio (run real en el anexo HAR-107):

```bash
docker buildx build --builder default \
  --platform linux/arm64 --load --progress plain \
  --build-arg "RUN_ID=$PAPERCLIP_RUN_ID" \
  -f platform/toolchain/Dockerfile \
  -t "usermanager03-toolchain:$PAPERCLIP_RUN_ID" .
docker run --rm --network none --user "$(id -u):$(id -g)" \
  --name "usermanager03-test-$PAPERCLIP_RUN_ID-verify" \
  --label com.hariklabs.project=usermanager03 \
  --label com.hariklabs.lifecycle=temporary \
  --label "com.hariklabs.run=$PAPERCLIP_RUN_ID" \
  --mount "type=bind,source=$PWD/platform/toolchain/verify.sh,target=/verify.sh,readonly" \
  "usermanager03-toolchain:$PAPERCLIP_RUN_ID" bash /verify.sh
```

El build inicial y la verificación finalizaron con exit 0. La revisión detectó
que el driver `docker-container` había creado un auxiliar privilegiado, contrario
a security.xml OPS_02. Se retiró el constructor y su volumen; el anexo conserva
el incidente y la reconstrucción correctiva mediante el driver `docker`. No
reutilizar el mecanismo anterior. La imagen se conserva como
dependencia del trabajo siguiente, con etiquetas de proyecto/run; su ID y el
inventario de recursos están en el anexo. No es una imagen de aplicación.

## Contrato y comprobaciones previas

Consultar `api/README.md`. Contrato de CRUD y auth en `api/openapi.yaml`;
configuración del generador en `platform/openapi/`. La generación de sondeo se
hizo en scratch; no se editó código generado ni se implementaron endpoints.

Comprobaciones reales: validación OpenAPI 7.26.0; generación Java/TypeScript;
compilación de interfaces/DTO con Java 25 y Boot 4.1.2; serialización Jackson 3
UTC y fronteras suplementarias; TypeScript 7.0.2; sondeo del proveedor de correo
Hibernate Validator 9.1.5.Final. Fuentes de los sondeos en
`platform/toolchain/email-probe/` y `platform/toolchain/generated-probe/`.
Registros, comandos completos y capturas se conservan en Paperclip HAR-107.

El sondeo no sustituye pruebas de aplicación contra PostgreSQL, sesión/CSRF,
concurrencia, UI ni QA de Seraph. Las fixtures de producto no son pruebas pasadas.
No hay comandos de despliegue verificados todavía. Compose, TLS, acceso David,
persistencia, healthchecks, Portainer y limpieza final continúan pendientes.

## Backend y pruebas verificadas

REST generado → mapper manual → servicio transaccional → JPA → PostgreSQL.
`backend/pom.xml` mantiene las versiones aprobadas. No editar `backend/target`;
Maven genera interfaces/DTO desde el contrato publicado. La fuente Unicode fijada
genera el recurso TSV mediante `platform/backend/generate-unicode.py`.

Verificados en HAR-107, run `49ae115d-1d5f-4267-a4a0-9da3232761c6`:
14 tests, 0 fallos/errores/omitidas, incluidos HTTP/CSRF, expiración, todos los puntos
Unicode, PostgreSQL real TLS y conflicto concurrente de DNI. Son pruebas de
desarrollo; no equivalen a matriz integral, QA independiente ni producto aceptado.

Reproducción del entorno efímero (desde raíz; requiere variables Paperclip reales):

```bash
python3 platform/testing/prepare.py "$PAPERCLIP_RUN_SCRATCH_DIR/test-secrets"
mkdir -p "$PAPERCLIP_RUN_SCRATCH_DIR/maven"
export UM03_UID="$(id -u)" UM03_GID="$(id -g)"
export UM03_TEST_SECRETS="$PAPERCLIP_RUN_SCRATCH_DIR/test-secrets"
export UM03_MAVEN_CACHE="$PAPERCLIP_RUN_SCRATCH_DIR/maven"
docker compose -p "usermanager03-test-$PAPERCLIP_RUN_ID" \
  -f platform/testing/compose.yaml up --abort-on-container-exit --exit-code-from verify
```

Los secretos sintéticos se generan en scratch privado, sin argumentos ni valores
impresos. PostgreSQL no publica puertos; datos tmpfs y roles migrador/aplicación
separados. El test JDBC prueba `verify-full`, UTF8 y rol sin DELETE. Fuera de ese
entorno `PostgresTest` se omite explícitamente; no confundir una ejecución sin BD
con el resultado integral anterior.

Tras preservar evidencia y verificar propiedad/ausencia de referencias protegidas,
se ejecutó la retirada temporal `docker compose -p "usermanager03-test-$PAPERCLIP_RUN_ID"
-f platform/testing/compose.yaml down`, sin `-v`. Nunca usar este procedimiento
para un despliegue permanente ni ejecutar prune global.

Logs de aplicación: JSON de una línea con timestamp UTC, level, application,
component, message y correlación. Flujos `operator_auth` (sesión/CSRF/login/logout),
`user_records` (CRUD/consulta) y `navigation` (recursos/UI); eventos request,
user_created, user_updated, user_deactivated y operation_failed. Cabecera
X-Correlation-ID limitada a caracteres seguros; el requestId de Problem lo genera
el servidor. Sin cuerpos, cookies, tokens ni valores personales en mensajes.

Pendientes: revisión/integración de UI Mouse, pruebas de navegador y matriz integral,
revisión de logging/dependencias/operación, TLS y acceso David, persistencia/reinicio,
Portainer y QA independiente. No hay comandos de despliegue permanente acreditados.

## Integración y operación verificadas (HAR-107)

Las notas de pendientes en secciones anteriores describen sus runs históricos.
La entrega actual integra React en Boot y ofrece `https://localhost:18443`, solo
loopback OCI. Certificado y claves reales permanecen fuera del repositorio.
`platform/runtime/compose.yaml` define servicios permanentes `usermanager03`,
restart unless-stopped, TLS interno verify-full, roles separados y volumen externo
protegido `usermanager03-postgres-data`. App y DB ejecutan UID/GID1001 verificados
en este host. No hay puerto DB publicado; health HTTPS/BD solo dentro de app.

Comandos ejecutados (variables no contienen valores de contraseña):

```bash
export UM03_PRIVATE_DIR=/opt/Dev/Secrets/UserManager03
export UM03_APP_IMAGE="usermanager03-app:$PAPERCLIP_RUN_ID"
docker buildx build --builder default --platform linux/arm64 --load \
  --build-arg "RUN_ID=$PAPERCLIP_RUN_ID" -f platform/runtime/Dockerfile \
  -t "$UM03_APP_IMAGE" .
# Solo NUEVA instalación; prepare.py rechaza sobrescritura y pide clave oculta.
python3 platform/runtime/prepare.py "$UM03_PRIVATE_DIR"
bash platform/runtime/operate.sh up
bash platform/runtime/operate.sh status
# Recreación autorizada comprobada sin pérdida; nunca down -v.
bash platform/runtime/operate.sh recreate
```

La preparación inicial del volumen externo se realizó con etiquetas project,
lifecycle=permanent y run real; inicialización de permisos1001/0700 con auxiliar
propio desechable. No repetir creación/preparación sobre datos existentes.
`prepare.py` produce una entrega privada inicial en `access/operator-password`;
no publicar ni copiar a tickets. La contraseña usada por Spring solo está en forma
Argon2id en `app/um03.operator.password-hash`. Tras entrega segura al operador,
retirar la copia de entrega mediante acción controlada. Cambiar cuenta con
`python3 platform/runtime/set-operator.py "$UM03_PRIVATE_DIR"` (entrada oculta),
y reiniciar app mediante `operate.sh restart-app`. La rotación fue comprobada:
clave anterior401, nueva204, sin modificar fichas; verificación Argon2id56.7ms.

Prueba navegador reproducible con dataset sintético nuevo (DNI no reutilizados):

```bash
docker buildx build --builder default --platform linux/arm64 --load \
  --build-arg "RUN_ID=$PAPERCLIP_RUN_ID" -f platform/browser/Dockerfile \
  -t "usermanager03-browser:$PAPERCLIP_RUN_ID" .
export UM03_EVIDENCE_DIR="$PAPERCLIP_RUN_SCRATCH_DIR/evidence"
export UM03_DATASET=76000000
bash platform/browser/run.sh
bash platform/browser/run.sh platform/browser/regression.cjs
```

Playwright1.64.1 y lockfile dedicado; fixture de regresión utiliza cuenta privada
montada solo lectura. No imprime cookies/tokens ni captura contraseñas. Chromium
confía en la clave específica del certificado; curl valida además certificado,
fechas y hostname y Node usa NODE_EXTRA_CA_CERTS. No se desactiva TLS globalmente.
El runner monta solo código, credencial de prueba y evidencia; nunca socket Docker.
Las fichas sintéticas creadas permanecen en la BD de demostración: no eliminarlas
con SQL ni limpiar el volumen permanente. QA puede usar su propio rango DNI.

Verificación real: escritorio1440×1000 y móvil390×844, CRUD/UTC/no-op/baja,
unicidad incluso inactivos, filtro/consulta/paginación, errores, separación de cuenta,
logout, recuperación403→401 sin repetición y detalle tardío. Las capturas y matriz
con límites por caso están en HAR-107. Recreación conservó recuento24 y checksum
completo; parada DB produjo readiness503 y luego healthy. TLS1.2 y1.3 verificaron
200 y ssl_verify_result0. Sin aceptación ni QA independiente.

Acceso de David: usar exclusivamente el alias SSH ya autorizado, StrictHostKeyChecking
activado y forwarding local18443. No abrir firewall ni publicar en0.0.0.0. El certificado público y procedimiento están en HAR-107. David confirmó
funcionamiento de acceso y Portainer mediante la interacción autenticada
6987f0ce-be0b-46db-aba5-4a604bbd18ec el 2026-10-26T17:11:38.570Z. No indicó
alias ni navegador/SO; Niobe no obtuvo sesión ni captura autenticada de Portainer.
Esta evidencia del operador queda diferenciada de la futura QA independiente.
No reconfigurar Portainer.

Tras recepción se retiró la copia privada `access/operator-password`; el hash de
runtime y el servicio permanecen intactos. Los comandos de navegador anteriores
describen la ejecución acreditada antes de esa retirada. Para nuevas pruebas
autenticadas, usar entorno aislado y credencial sintética privada propios; no
rotar la cuenta de David ni recrear la copia de entrega. La imagen de navegador
retenida usa el tag del run `dfe7ab16-7266-4572-a30f-a7edf940e910`; el runner
construye el tag a partir del run actual, por lo que este requiere construir o
etiquetar su imagen de prueba antes de ejecutarlo.
