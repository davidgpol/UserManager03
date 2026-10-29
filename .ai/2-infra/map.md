# UserManager03 — Mapa de paquetes y carpetas

## API

Raíz: `api/`.
Contiene la definición del contrato público y sus datos de referencia.

| Carpeta | Responsabilidad |
|---|---|
| Raíz de `api/` | Contrato OpenAPI y documentación del contrato. |
| `fixtures/` | Casos de referencia para validaciones de correo, Unicode, DNI, consultas y errores. |
| `unicode/` | Fuente de categorías Unicode utilizada por las validaciones y su licencia. |

## Frontend

Raíz: `frontend/`.
Contiene la aplicación React y la configuración de TypeScript y Vite.

| Carpeta | Responsabilidad |
|---|---|
| `src/` | Arranque de React, interfaz de usuario, integración con la API, validaciones y recuperación de sesión. |
| `src/generated/api/` | Cliente TypeScript generado y soporte de comunicación HTTP. |
| `src/generated/api/apis/` | Operaciones generadas para las API de usuarios y autenticación. |
| `src/generated/api/models/` | Tipos y conversiones de los modelos del contrato OpenAPI. |

## Backend

Raíz: `backend/`.
Contiene la aplicación Spring Boot y su configuración de build.

### Código de aplicación

Raíz: `backend/src/main/java/com/hariklabs/usermanager03/`.
Paquete base: `com.hariklabs.usermanager03`.

| Paquete o carpeta | Responsabilidad |
|---|---|
| Paquete base | Arranque de la aplicación Spring Boot. |
| `rest/` | Controladores HTTP de usuarios y autenticación, navegación hacia la interfaz web y tratamiento de errores HTTP. |
| `rest/mapper/` | Conversión entre los modelos HTTP y los modelos de `service/model/`. |
| `service/` | Validación de dominio, categorías Unicode y errores de negocio. |
| `service/api/` | Interfaces que definen las operaciones de negocio disponibles para la capa REST. |
| `service/impl/` | Implementaciones de los contratos de servicio, lógica de negocio y límites transaccionales. |
| `service/model/` | Modelos internos de entrada, consulta y resultado utilizados por los contratos de servicio. |
| `persistence/` | Entidades JPA e interfaces de repositorio cuya implementación proporciona Spring Data JPA. |
| `security/` | Configuración de autenticación, protección de peticiones y gestión de los límites de sesión. |
| `config/` | Configuración web, procesamiento JSON, correlación de peticiones y formato de logs. |

### Código Java generado

Raíz: `backend/target/generated-sources/openapi/src/main/java/`.

| Paquete | Responsabilidad |
|---|---|
| `com.hariklabs.usermanager03.rest.generated.api` | Interfaces HTTP generadas desde el contrato OpenAPI. |
| `com.hariklabs.usermanager03.rest.generated.model` | Modelos de peticiones y respuestas generados desde el contrato OpenAPI. |

### Recursos

Raíz: `backend/src/main/resources/`.

| Carpeta | Responsabilidad |
|---|---|
| Raíz de recursos | Configuración de la aplicación, configuración de logging y tabla de categorías Unicode. |
| `db/migration/` | Migraciones SQL de Flyway para el esquema de base de datos. |

### Pruebas

Raíz: `backend/src/test/java/com/hariklabs/usermanager03/`.

| Paquete | Responsabilidad |
|---|---|
| `config/` | Pruebas del formato y saneamiento de logs. |
| `rest/` | Pruebas del comportamiento HTTP y del contrato de la API. |
| `security/` | Pruebas de la política de sesiones. |
| `service/` | Pruebas de dominio y de integración con PostgreSQL. |

## Platform

Raíz: `platform/`.
Contiene la infraestructura, los entornos de ejecución y las herramientas del proyecto.

| Carpeta | Responsabilidad |
|---|---|
| `backend/` | Generación de la tabla Unicode del backend y ejecución de comprobaciones Maven. |
| `frontend/` | Generación del cliente API y de la tabla Unicode del frontend, y ejecución de sus comprobaciones. |
| `openapi/` | Configuración y scripts de generación de código Java y TypeScript desde OpenAPI. |
| `runtime/` | Imagen de aplicación, despliegue Compose, configuración de PostgreSQL y herramientas de preparación y operación. |
| `testing/` | Entornos aislados de prueba, preparación de datos y credenciales sintéticos, regresiones HTTP y auditoría de logs. |
| `browser/` | Entorno y pruebas de navegador con Playwright. |
| `toolchain/` | Imagen y herramientas de compilación, generación y verificación de versiones. |
| `toolchain/email-probe/` | Comprobación del comportamiento del validador de correo. |
| `toolchain/generated-probe/` | Comprobación de compilación y serialización de modelos generados. |
