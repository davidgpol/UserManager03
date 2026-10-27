# UserManager03 — Lessons Learned

## Purpose

Preserve verified project knowledge useful for future work.
Execution history belongs in `progress.log`; this document records reusable conclusions.

## Recording criteria

- Record one learning per entry.
- Support each learning with traceable evidence.
- Distinguish observed behavior from an explanation of its cause.
- State the conditions under which the learning applies.
- Keep unresolved hypotheses in the associated Paperclip issue.
- Mark outdated entries as superseded while preserving their references.
- A lesson does not override project.xml, general.xml or security.xml.

## Lessons



### HAR-107 — Generación Unicode y mecanismo Docker

- OpenAPI Generator 7.26.0 traduce maxLength de nombres de respuesta a @Size,
  que usa unidades UTF-16. Conservar límites por puntos de código en la extensión
  Unicode del contrato y validar dominio explícitamente; el sondeo de DTO generado
  demuestra fronteras suplementarias válidas tras la corrección. No editar generado.
- El driver docker-container de Buildx crea un auxiliar con Privileged=true aunque
  no se pase esa opción. Es incompatible con security.xml OPS_02. Se retiró el
  auxiliar propio y su volumen; usar el driver docker existente para este proyecto.
  La capacidad técnica de Buildx no exceptúa las reglas de seguridad.
- El POM de ejemplo del generador usa Boot 4.0.1/Java 17; no define la identidad
  del proyecto. Generar solo interfaces/modelos y compilar con el POM normativo.

Evidencia: HAR-107, run 7e663ce2-43fc-488e-a1ee-d1a44950d594; registros de generación,
compilación, SerializationProbe y revisión HostConfig en su anexo. Estas observaciones
no equivalen a QA independiente ni validación funcional de la aplicación.

### HAR-107 — Verificación HTTP y Unicode con PostgreSQL

- En pruebas MockMvc, suministrar sesión no representa automáticamente una cookie
  previa válida: declarar requestedSessionId y su validez para verificar rotación
  real del mecanismo estándar de Spring. Evidencia HttpContractTest.
- El borrado mediante CookieClearingLogoutHandler no conservó SameSite=Lax en la
  verificación. El handler de éxito usa ResponseCookie explícito conservando los
  atributos; la invalidación/contexto siguen en el filtro estándar.
- Las interfaces generadas incluyen medios de error en produces. La negociación
  de éxito se acota a application/json y 406 publica Problem solo cuando el cliente
  lo admite. HttpContractTest verifica ambos casos; no editar código generado.

Resultados del run 49ae115d-1d5f-4267-a4a0-9da3232761c6; no sustituyen QA.

### HAR-107 — Integración navegador y operación

- Los localizadores exactos por etiqueta MUI deben considerar el marcador required;
  validar selectores en navegador real, no inferir desde JSX.
- La confianza Chromium mediante pin de clave no configura el cliente Node de
  route.fetch. Proporcionar NODE_EXTRA_CA_CERTS con certificado del proyecto; los
  errores de red pueden incluir cabeceras, por lo que deben redactarse antes de
  publicar y las sesiones involucradas invalidarse.
- Una tabla sin desbordamiento global aún puede ser ilegible en móvil. La inspección
  de captura390px motivó tarjetas con acciones visibles y prueba de apertura real.
- Compose interpreta comas de listas YAML inline: citar completamente opciones
  tmpfs o usar listas de bloque. El fallo se produjo antes de arrancar aplicación.

Evidencia: HAR-107, run dfe7ab16-7266-4572-a30f-a7edf940e910. No reemplaza QA.


### HAR-110 — Conflictos y privacidad de logs

- PostgreSQL emite el valor de UNIQUE en DETAIL; reducir parámetros por sí solo
  no lo elimina. El perfil compartido usa terse y omite sentencias/parámetros,
  conservando severidad, SQLSTATE y sesión. Verificado con conflicto real y SHOW.
- Hibernate7 usa `org.hibernate.orm.jdbc.error`; apagar solo el logger legacy
  SqlExceptionHelper no evita exponer detalles JDBC. El encoder sanea ambas
  categorías conservando mensajes técnicos seguros; no se apaga el logger.
- El scratch de un run fallido puede desaparecer mientras sobreviven contenedores
  detenidos con mounts a esa ruta. Preservar entregables en Paperclip; no asumir
  que una ruta del historial aún existe. HAR-110 reprodujo evidencia tras esa pérdida.

Evidencia: HAR-110 backend19 PASS, HTTPS409/concurrencia, 82 marcadores sin
coincidencias en logs app/BD. No sustituye HAR-111 ni borra el FAIL HAR-108.


### HAR-112 — Identificación y comparación durante limpieza

- La recreación de QA dejó app/DB/red permanentes con el mismo label run que sus
  temporales. El run por sí solo no autoriza retirar: contrastar nombres/IDs,
  proyecto Compose, lifecycle, montajes y referencias conforme AUTH_08/OPS_07.
- El JSON de `docker image ls --format '{{json .}}'` incluye `Containers`, que
  disminuye al retirar contenedores sin cambiar la imagen. Para comprobar
  preservación comparar ID, Repository, Tag y Digest, no ese contador de uso.

Evidencia: HAR-112, inventarios antes/después y comparación de imágenes; los únicos
campos distintos fueron contadores de uso. Cuatro contenedores y dos redes retirados,
permanentes e imágenes preservados. No sustituye la QA independiente HAR-113.
