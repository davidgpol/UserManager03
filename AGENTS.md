# UserManager03 — Guía del repositorio

## 1. Propósito del proyecto

Aplicación web de gestión de usuarios con autenticación mediante
una cuenta operadora independiente.

El proyecto comprende un contrato REST, un frontend React, un backend
Spring Boot y la infraestructura necesaria para su ejecución.

La identidad tecnológica y arquitectónica se define en
`.ai/0-core/project.xml`.

## 2. Lectura obligatoria del contexto del proyecto

Estas instrucciones se aplican una vez establecida la documentación
técnica del proyecto.

El primer paso de cualquier trabajo será leer los siguientes archivos
vigentes del repositorio, respetando este orden:

| Prioridad | Documento | Contenido |
|---|---|---|
| P0 — Máxima | `.ai/0-core/project.xml` | Identidad esencial, tecnologías, versiones y arquitectura del proyecto. |
| P1 | `.ai/1-rules/general.xml` | Reglas de implementación, desarrollo y logging. |
| P2 | `.ai/1-rules/security.xml` | Requisitos de seguridad. |
| P3 | `.ai/2-infra/map.md` | Estructura de paquetes y carpetas, con sus responsabilidades. |
| P4 | `.ai/3-runtime/progress.log` | Historial cronológico de cambios, resultados y verificaciones. |
| P5 | `.ai/3-runtime/lessons_learned.md` | Aprendizajes técnicos reutilizables y sus referencias. |

Esta lectura es obligatoria antes de analizar, diseñar, construir,
modificar, probar o revisar cualquier parte del proyecto.

Debe realizarse en cada nueva ejecución y al retomar un trabajo.
Si alguno de estos documentos cambia durante la ejecución, volver
a leer su versión actualizada antes de continuar con el trabajo afectado.

Estos documentos constituyen el contexto esencial del proyecto.
Sus definiciones, restricciones y aprendizajes deben orientar las
decisiones durante todo el trabajo, respetando la función y autoridad
de cada documento.

## 3. Autoridad y prioridad de los documentos

El orden establecido en el apartado anterior determina tanto
la lectura como la precedencia:

project.xml > general.xml > security.xml > map.md > progress.log > lessons_learned.md

### Regla de precedencia

Ante una contradicción entre dos documentos, prevalece siempre
el de mayor prioridad.

Un documento de menor prioridad puede complementar al superior
dentro de su ámbito, pero no modificarlo, contradecirlo ni
introducir excepciones a sus disposiciones.

La fecha más reciente, el mayor nivel de detalle o la existencia
de código que siga el documento inferior no alteran esta prioridad.

### Función de cada documento

- `project.xml` contiene la identidad inmutable del proyecto.
  Su modificación requiere una decisión explícita sobre el cambio
  concreto de identidad.

- Los descriptores de build y el código deben implementar esa identidad.
  Una discrepancia de la implementación no justifica modificar
  automáticamente `project.xml`.

- `general.xml` contiene las reglas de implementación, desarrollo
  y logging, subordinadas a la identidad definida en `project.xml`.

- `security.xml` contiene los requisitos de seguridad, respetando
  la precedencia de `project.xml` y `general.xml`.

- `map.md` describe la organización de paquetes y carpetas.
  Debe mantenerse alineado con la estructura implementada.
  No puede redefinir la arquitectura ni introducir nuevas reglas.

- `progress.log` registra hechos, cambios y verificaciones.
  Sus entradas no conceden autorizaciones ni modifican las
  definiciones normativas.

- `lessons_learned.md` recoge aprendizajes técnicos.
  Sus entradas no sustituyen ni modifican las reglas vigentes.

La prioridad no amplía la función de un documento.

### Actuación ante contradicciones

- Identificar los documentos y apartados afectados.
  Citar el identificador de la regla cuando exista.

- Aplicar la disposición del documento de mayor prioridad.

- Comunicar la discrepancia encontrada.

- Conservar las demás disposiciones compatibles del documento inferior.

- No reescribir los documentos para resolver la discrepancia
  sin autorización para ese cambio.

Si la contradicción está dentro de un mismo documento o no puede
resolverse mediante esta jerarquía, solicitar una aclaración antes
de ejecutar la parte afectada.

Continuar con el trabajo que no dependa de esa decisión.

Los hechos históricos se conservan como tales, aunque describan
una implementación que no cumplía las reglas vigentes.

### Conservación de la documentación .ai

Estas instrucciones se aplican una vez establecida la documentación
del proyecto.

- Conserva las etiquetas, atributos, secciones y campos establecidos
  en cada documento.

- No añadas, elimines, renombres ni reorganices esos elementos
  sin una instrucción explícita que autorice el cambio concreto.

- `project.xml` contiene la identidad inmutable del proyecto.
  No modifiques sus valores para adaptarlos al código existente
  ni para incorporar decisiones de implementación.

- En `general.xml` y `security.xml`, no añadas, elimines ni cambies
  obligaciones sin una instrucción explícita sobre esas reglas.
  Tampoco introduzcas nuevas obligaciones mediante comentarios
  o texto libre.

- Mantén `map.md` actualizado con las carpetas, paquetes y
  responsabilidades correspondientes, utilizando su formato existente.

- Añade hechos a `progress.log` y aprendizajes contrastados a
  `lessons_learned.md` utilizando los campos establecidos.
  Estas entradas no pueden introducir ni modificar reglas del proyecto.

- Una petición de implementar una funcionalidad no autoriza por sí
  misma a ampliar o modificar la documentación normativa.

- Si una necesidad requiere cambiar la estructura o las reglas,
  presenta el cambio concreto y su motivo antes de aplicarlo.
  Continúa con el trabajo que no dependa de esa decisión.

## 4. Trabajo técnico y verificación

### Contrato y código

- Consultar el contrato REST en `api/openapi.yaml` antes de modificar
  el comportamiento HTTP.

- Utilizar la configuración de `platform/openapi/` para la generación
  de interfaces, modelos y cliente API.

- Realizar las correcciones del código generado en el contrato
  o en la configuración del generador.

- Aplicar la arquitectura por capas e Interface-Driven definida
  en `project.xml`.

- Localizar los componentes de API, Frontend, Backend y Platform
  mediante `.ai/2-infra/map.md`.

### Infraestructura

- Mantener los Dockerfiles, archivos Compose, configuraciones de
  infraestructura y scripts operativos bajo `platform/`.

- Ejecutar builds y pruebas mediante los entornos Docker
  definidos por el proyecto.

- Utilizar la raíz del repositorio como contexto de construcción
  cuando el Dockerfile necesite acceder a varios componentes.

- Consultar los procedimientos del README y los scripts correspondientes,
  comprobando que se ajustan a la configuración vigente.

### Comprobaciones

- Comprobar la existencia y el contenido de los archivos utilizados.

- Verificar los cambios mediante las pruebas y comprobaciones
  correspondientes a los componentes afectados.

- Distinguir entre contenido inspeccionado, funcionalidad implementada,
  pruebas ejecutadas y comportamiento observado en ejecución.

- Identificar las comprobaciones fallidas y las que no se hayan ejecutado.

- Respaldar los resultados comunicados con evidencias localizables.

- La existencia de una prueba o de un commit no demuestra
  que una ejecución haya resultado satisfactoria.

- Antes de dar por terminado un cambio, comprobar su conformidad
  con la identidad y las reglas aplicables, dejando constancia de
  las verificaciones realizadas y las discrepancias pendientes.

## 5. Mantenimiento de la documentación

Actualizar los documentos afectados por los cambios realizados:

- `map.md`: actualizar las ubicaciones y responsabilidades cuando
  cambie la estructura de paquetes o carpetas.

- `progress.log`: añadir los hitos técnicos con fecha UTC, áreas
  afectadas, cambio, resultado, verificación, referencias y limitaciones.

- `lessons_learned.md`: incorporar aprendizajes técnicos contrastados,
  describiendo el problema, la solución aplicable y su referencia.

- `README.md`: mantener los procedimientos de preparación, ejecución,
  pruebas y operación correspondientes a la configuración vigente.

Conservar los hechos históricos al actualizar formatos o referencias.
Registrar las correcciones de resultados anteriores mediante nuevas
entradas que permitan identificar el hecho corregido.

Mantener las referencias actualizadas cuando se muevan archivos.
Distinguir las referencias al código de las evidencias de ejecución.
