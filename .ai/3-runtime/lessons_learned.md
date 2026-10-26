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
