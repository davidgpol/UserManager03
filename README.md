# UserManager03

Gestión de fichas de usuarios y cuenta operadora independiente. Identidad y
versiones normativas en `.ai/0-core/project.xml`; instrucciones en `AGENTS.md`.

Estado de HAR-107: toolchain ARM64 construido/verificado y contrato OpenAPI
validado y publicado en `4b282103f7c4d1f7d9399c184cd4c408b44f9c29`
mediante Git y helper `gh` de OCI (SHA remoto verificado). No hay
aplicación, servicio HTTPS, despliegue persistente ni aceptación funcional.

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
