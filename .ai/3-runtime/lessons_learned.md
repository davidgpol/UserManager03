# UserManager03 — Lecciones aprendidas

Aprendizajes técnicos del proyecto que permiten evitar errores ya identificados.

Cada entrada describe un problema concreto, la solución aplicable y una
referencia técnica del repositorio. Las referencias a pruebas identifican
dónde se comprueba el comportamiento; no certifican su última ejecución.

Las rutas indicadas son relativas a la raíz del repositorio.

## Longitud de nombres y caracteres Unicode

**Problema:** La validación generada mediante @Size puede medir unidades
UTF-16, mientras que el contrato del proyecto limita puntos de código.
Esto puede rechazar nombres válidos que contienen caracteres suplementarios.

**Aprendizaje:** Mantener los límites de nombres en la extensión Unicode
del contrato y aplicar la validación explícita del dominio. Comprobar
también los modelos generados con caracteres suplementarios.

**Referencias:**
- `api/openapi.yaml`
- `api/fixtures/unicode.json`
- `platform/toolchain/generated-probe/SerializationProbe.java`

## Versiones del proyecto y código generado

**Problema:** Las versiones predeterminadas de los proyectos de ejemplo
generados pueden diferir de la identidad tecnológica del proyecto.

**Aprendizaje:** Compilar las interfaces y modelos generados mediante el
descriptor de build propio, que implementa las versiones de project.xml.

**Referencias:**
- `backend/pom.xml`
- `platform/openapi/spring.json`
- `platform/toolchain/generated-probe/pom.xml`

## Rotación de sesión en pruebas MockMvc

**Problema:** Asociar un objeto de sesión a una petición de prueba no
representa por sí solo una petición que aporta un identificador de sesión
previo válido.

**Aprendizaje:** Para comprobar la rotación tras el login, configurar el
identificador solicitado y su validez, y comparar el identificador de
sesión anterior con el posterior.

**Referencia:**
- `backend/src/test/java/com/hariklabs/usermanager03/rest/HttpContractTest.java`,
  método `login()`.

## Atributos de la cookie al cerrar sesión

**Problema:** La respuesta de logout debe comprobarse expresamente;
invalidar la sesión no demuestra que la cabecera de borrado conserve
los atributos esperados de la cookie.

**Aprendizaje:** El proyecto construye explícitamente la cookie de
borrado con sus atributos. La prueba de logout comprueba SameSite=Lax
y la invalidación de la sesión.

**Referencias:**
- `backend/src/main/java/com/hariklabs/usermanager03/security/SecurityConfiguration.java`
- `backend/src/test/java/com/hariklabs/usermanager03/rest/HttpContractTest.java`

## Negociación de respuestas JSON

**Problema:** Las declaraciones de medios generadas desde OpenAPI pueden
incluir tanto el formato de éxito como el de error. Esa declaración no
garantiza por sí sola la negociación requerida por cada respuesta.

**Aprendizaje:** Verificar separadamente application/json,
application/problem+json y los medios no admitidos. En este contrato,
una respuesta 406 no incluye cuerpo si el cliente excluye
application/problem+json.

**Referencias:**
- `api/openapi.yaml`
- `backend/src/test/java/com/hariklabs/usermanager03/rest/HttpContractTest.java`,
  prueba `responseNegotiationPreservesJsonAndProblemTypes()`.

## Selectores de campos obligatorios en MUI

**Problema:** El marcador de campo obligatorio puede afectar al nombre
accesible utilizado por los localizadores de las pruebas.

**Aprendizaje:** Elegir los localizadores a partir del componente
renderizado y comprobar que identifican el campo correcto.

**Referencias:**
- `frontend/src/App.tsx`
- `platform/browser/regression.cjs`

## Usabilidad de la lista en móvil

**Problema:** Una vista puede carecer de desbordamiento horizontal y
seguir mostrando información o acciones difíciles de utilizar.

**Aprendizaje:** Comprobar la legibilidad y las acciones en un ancho
móvil real. El proyecto utiliza una presentación adaptada y comprueba
la apertura del detalle desde esa vista.

**Referencias:**
- `frontend/src/App.tsx`
- `platform/browser/smoke.cjs`

## Confianza TLS del navegador y del proceso Node

**Problema:** La configuración de confianza aplicada a Chromium no
configura automáticamente la confianza TLS del proceso Node que
ejecuta las pruebas.

**Aprendizaje:** Configurar también el certificado de confianza para
el proceso Node. El runner del proyecto utiliza NODE_EXTRA_CA_CERTS
para ese contexto.

**Referencias:**
- `platform/browser/run.sh`
- `platform/browser/regression.cjs`

## Datos personales en errores de PostgreSQL

**Problema:** Los detalles de una violación de unicidad pueden incluir
el valor que ha provocado el conflicto. Desactivar el registro de
parámetros no cubre por sí solo todos los detalles del error.

**Aprendizaje:** Revisar conjuntamente la verbosidad de errores,
el registro de sentencias y el de parámetros. La configuración del
proyecto conserva información diagnóstica como SQLSTATE y evita
registrar los valores de las filas en esos detalles.

**Referencias:**
- `platform/runtime/postgres-logging.conf`
- `platform/testing/audit-logs.py`

## Categorías de logging JDBC de Hibernate

**Problema:** Sanear únicamente la categoría histórica
SqlExceptionHelper deja sin cubrir la categoría
org.hibernate.orm.jdbc.error utilizada por Hibernate 7.

**Aprendizaje:** El encoder contempla ambas categorías y sustituye
los mensajes sensibles, conservando los campos diagnósticos seguros.

**Referencias:**
- `backend/src/main/java/com/hariklabs/usermanager03/config/SafeJsonEncoder.java`
- `backend/src/test/java/com/hariklabs/usermanager03/config/SafeJsonEncoderTest.java`

## Opciones tmpfs en Compose

**Problema:** Las comas de las opciones tmpfs pueden interpretarse
como separadores cuando se escriben dentro de una lista YAML inline
sin entrecomillar el elemento completo.

**Aprendizaje:** Utilizar una lista de bloque o entrecomillar
completamente cada elemento tmpfs de una lista inline.

**Referencias:**
- `platform/runtime/compose.yaml`
- `platform/testing/compose.yaml`

## Identificación de recursos Docker temporales

**Problema:** Un identificador de ejecución puede estar presente tanto
en recursos temporales como en recursos permanentes.

**Aprendizaje:** Determinar el carácter temporal mediante la identidad
del proyecto, la etiqueta lifecycle y los montajes o dependencias del
recurso. El identificador de ejecución, por sí solo, no permite decidir
qué puede eliminarse.

**Referencias:**
- `platform/runtime/compose.yaml`
- `platform/testing/compose.yaml`
- `platform/runtime/verify-recreation.py`

## Contrato de servicio y modelos internos

**Problema:** Un controlador que inyecta una implementación concreta y un mapper
que consume records anidados de ella no mantiene la frontera Interface-Driven.
Una consulta interna que interpreta directamente `Map<String,String[]>` también
conserva un detalle HTTP fuera del mapper.

**Aprendizaje:** Exponer un contrato de servicio con modelos internos propios,
mantener lógica y límites transaccionales en su implementación, y convertir los
parámetros HTTP en `rest/mapper/`. Comprobar en Spring el proxy del contrato y
los atributos transaccionales de la implementación contra PostgreSQL real.

**Referencias:**
- `backend/src/main/java/com/hariklabs/usermanager03/service/api/UserService.java`
- `backend/src/main/java/com/hariklabs/usermanager03/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/hariklabs/usermanager03/service/model/`
- `backend/src/main/java/com/hariklabs/usermanager03/rest/mapper/UserRestMapper.java`
- `backend/src/test/java/com/hariklabs/usermanager03/service/PostgresTest.java`
