# Correcciones de revisión HAR-109

Fecha: 2026-10-26. Alcance: correcciones solicitadas por Niobe sobre
`frontend/` y `platform/frontend/`; sin cambios en backend, contrato, mapa o
registro compartido.

## Cambios realizados

- `tsconfig.json` conserva `strict: true` y activa `noImplicitAny: true`.
  `App.tsx` ya no emplea alias `any` de componentes Material UI.
- La validación Unicode compara correctamente la familia de categoría
  (`Ll`/`Lu` pertenecen a `L`). Se añadió la comprobación de texto pegado:
  antes de que el navegador lo inserte, rechaza controles, sustitutos aislados y
  categorías no permitidas, sin limpiar ni truncar el contenido válido.
- La pérdida de sesión limpia fichas, selección, detalle, edición, baja, filtros,
  paginación y token en memoria. La contraseña se borra al terminar cada intento
  de acceso.
- Las consultas de listado usan una secuencia local: una respuesta antigua no
  puede sobrescribir filtros, búsqueda o página posteriores. Al cargar o fallar
  no se presenta una tabla obsoleta.
- La baja tiene estado pendiente, doble envío deshabilitado, error visible y
  reintento explícito. El cierre de sesión conserva el estado si falla; solo
  confirma el cierre tras respuesta satisfactoria.
- Cada fila tiene acciones de detalle, edición y baja accesibles por botón. El
  detalle consulta `getUser` y presenta identificador, estado y auditoría UTC,
  con carga, error y reintento.
- Los errores Problem Details se traducen por `code` para conflicto de DNI,
  validación, CSRF, sesión y disponibilidad. La interfaz se adapta a pantallas
  estrechas y permite ruptura de nombres/correos largos.
- `platform/frontend/run.sh` acepta únicamente `generate` o `verify`,
  pasa argumentos posicionales al contenedor, usa `bash` explícito y evita
  `bash -l` e interpolar el comando.

## Verificación ejecutada

```text
PAPERCLIP_RUN_ID=$PAPERCLIP_RUN_ID platform/frontend/run.sh verify
```

Resultado: exit 0 en la imagen autorizada
`usermanager03-toolchain:7e663ce2-43fc-488e-a1ee-d1a44950d594`.

Casos comprobados:

- generación de la tabla Unicode 16.0.0;
- prueba de validación, incluido pegado válido y rechazo de tabulador/NUL;
- `tsc --noEmit` con comprobación estricta;
- build de producción Vite.

Vite informa solamente del aviso de tamaño de chunk (597.21 kB sin comprimir);
no se cambió división de bundles porque excedería esta corrección acotada.

## Limitación

No se ejecutó integración navegador/backend ni QA independiente. Niobe conserva
la revisión e integración final en [HAR-107](/HAR/issues/HAR-107).

## Segunda revisión de Niobe — corrección adicional

- `canonicalDni` ya no emplea `String.trim()`: recorta exclusivamente los
  controles U+0009..U+000D/U+001C..U+001F y categorías UCD 16.0 `Zs`, `Zl` y
  `Zp`, como `UserValues.trimSpaces`. U+FEFF permanece y por tanto no se
  transforma silenciosamente en un DNI aceptable. El recorte de nombres se
  mantiene separado y limitado a `Zs`, como `UnicodeCategories.trimZs`.
- `validateName` opera por índices UTF-16 y puntos individuales; no expande un
  array en `String.fromCodePoint`. Conserva el valor completo, incluidos más de
  200000 puntos, y devuelve el error de rango sin truncarlo ni lanzar
  `RangeError`.
- La recuperación de un 403 conserva el error original de la mutación, renueva
  CSRF y consulta la sesión sin repetir la mutación. Todas las llamadas de
  auth/session pasan por el mismo manejador 401, que borra token y estado
  privado. Tras logout satisfactorio se solicita el token CSRF nuevo del
  contrato.
- Detalle, guardado y baja incorporan una época de sesión; cerrar detalle,
  abrir otro, logout o 401 invalida respuestas pendientes para que no puedan
  restaurar fichas o diálogos de una sesión anterior.
- Se conserva el correo literalmente para Jakarta Email del servidor: no se
  añadió `type=email`, normalización ni restricción adicional en cliente.

## Verificación adicional

`platform/frontend/run.sh verify` terminó con exit 0 usando exclusivamente la
imagen autorizada. La prueba ejecutó los 47 casos de `unicode.json`, los 9 DNI
de `dni-query-errors.json`, los 10 valores de `email.json`, una entrada de
200001 puntos y la frontera con `Zs`. También comparó `categoryOf` generado
contra cada uno de los 1114112 puntos de
`DerivedGeneralCategory-16.0.0.txt`. Incluye un fetch mock 403 → CSRF 200 →
sesión 401 que verifica limpieza del estado y conservación del 403 original.

Además pasaron `tsc --noEmit`, Vite build y `git diff --check`. Vite mantiene
el único aviso no bloqueante de chunk de 598.31 kB. No hubo integración
navegador/backend ni QA independiente.
