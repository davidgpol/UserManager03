# Contrato UserManager03

OpenAPI 3.0.3 en `openapi.yaml`, preparado antes de endpoints. Publicación remota
pendiente de la conexión GitHub de HAR-107; no hay implementación de endpoints.

Fuentes: project.xml S02-NIOBE-3P; HAR-84 revisión
`97c325a4-4516-463b-bb07-500a0bcb80ac`; HAR-86 revisión
`e8e12e4e-90c2-4657-bc58-9a3f1095f9c1`; HAR-96 revisión
`cd64f9ae-11bf-424d-9272-f3f9b937f288`.

`fixtures/unicode.json` contiene 47 expectativas discriminantes, incluyendo
sustitutos aislados, formatos, espacios Zs y límites de puntos suplementarios.
`fixtures/dni-query-errors.json` conserva expectativas de DNI, consulta, seguridad,
errores y operaciones sin cambio. Son expectativas contractuales, no pruebas de
producto pasadas. `fixtures/email.json` distingue el resultado real del sondeo
del proveedor `@Email` de la obligatoriedad que deberá aplicar el servicio.

La fuente Unicode 16.0.0 y licencia se conservan bajo `unicode/`. Su SHA-256
normativo es `7676ab755a41ef82108460238569e60ad65c191ddafe61b36c6765ec1353f293`.
Se verificaron 4099 rangos inclusivos, sin huecos ni solapes, para 1114112 puntos.
Las tablas Java/TypeScript del producto siguen pendientes.

Revisión técnica realizada en HAR-107:

- El generador 7.26.0 valida el contrato y genera interfaces/DTO y cliente.
- Los límites de nombre se expresan mediante `x-usermanager-unicode` tanto en
  entrada como en salida. `maxLength` en salida producía `@Size` y aplicaba
  unidades UTF-16; se corrigió el contrato, sin editar los archivos generados.
- Los DTO aceptan las fronteras suplementarias en Bean Validation; la validación
  completa de categorías y dominio sigue siendo responsabilidad del servicio.
- Los DTO/interfaces compilan con `--release 25`, Boot 4.1.2 y Jackson 3.1.7.
  El sondeo de serialización conserva fechas UTC con microsegundos y valores.
- El cliente pasa TypeScript 7.0.2 con `tsc --noEmit --strict`.
- El POM de ejemplo emitido por el generador usa versiones ajenas a la baseline;
  no se usa como descriptor del proyecto. El script limita la generación Java
  a interfaces/modelos. Los POM de los sondeos implementan las versiones aprobadas.
- Se observaron avisos de anotaciones obsoletas en código generado; no impiden
  compilar. La prueba integrada de aplicación, seguridad y PostgreSQL está pendiente.

Las condiciones USR-06, QRY-09, MUT-10 y MUT-11 permanecen pendientes: no hay
contrato publicado ni UI implementada. Los 56 RE/CD tampoco se marcan pasados.
No se ha ejecutado QA independiente ni se declara aceptación.
