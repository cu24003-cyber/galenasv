# Idiomas de GalenoSV

La configuración común está en `src/main/webapp/WEB-INF/faces-config.xml`:
`i18n.Messages` se publica como `msg`. Todas las vistas deben usar
`/WEB-INF/templates/template.xhtml`; no necesitan `f:loadBundle` propio.

`IdiomaBean` es CDI `@SessionScoped` y serializable. Inicia en español, admite
es/en/fr/pt/zh y actualiza el locale de la vista al cambiar el selector.
Cada sesión tiene su propia selección. La plantilla aplica también el atributo
HTML `lang` y carga los recursos de idioma incluidos en PrimeFaces 16.
El selector hace un POST completo desde su propio formulario: no valida ni guarda
los formularios de edición. Los valores todavía no enviados del formulario de
edición no deben considerarse guardados al cambiar de idioma.

## Añadir textos

1. Añadir la misma clave a los bundles base, es, en, fr, pt y zh en UTF-8.
2. Mantener `Messages_es.properties` idéntico al base español. El bundle explícito
   evita que una JVM en inglés elija inglés como fallback para `es`.
3. En XHTML usar, por ejemplo, `value="#{msg['boton.guardar']}"`.
4. En modelos Java usar `Mensajes.texto("error.guardar")`. Para errores de servicio,
   `Mensajes.detalle(e)` obtiene la traducción; los diagnósticos quedan en el log.
5. Para un nuevo error de negocio, usar el constructor de `ServiceException` con
   clave, diagnóstico y causa. No introducir dependencias de Faces en servicios.
   La anotación `@ApplicationException(rollback = true)` conserva el error de
   negocio al cruzar EJB y marca la transacción para rollback.

Los nombres de idiomas se muestran en su propio idioma. Los datos de personas,
roles y demás entidades no se traducen. Se conserva el formato de fecha dd/MM/yyyy.

## Verificación

Ejecutar `mvn test` (o `mvn package`). `I18nTest` comprueba las claves en todos los
idiomas, igualdad de ambos bundles españoles, UTF-8, fallback español, serialización,
aislamiento de sesiones, actualización del locale y resolución de mensajes Java.

Comprobado en Liberty y navegador:

- Selector en los cinco idiomas; tabla y paginación traducidas.
- Selección francesa conservada entre Ejemplo, Personas y Roles.
- Formulario de persona, calendario y validaciones obligatorias en francés.
- No se crearon, modificaron ni eliminaron registros durante la comprobación.

El entorno de esta comprobación usa Java 25 y compila con release 21 según el POM.
Conviene repetir el despliegue en el Java 21 utilizado por el equipo.

## Correcciones de formularios

Se implementó `PersonaModel.eliminar`, con manejo traducido de errores y recarga
solo tras una eliminación exitosa. Roles incluye tabla, creación, edición sobre
una copia, cancelación, validación, guardado y eliminación con confirmación.
Los errores conservan abierto el formulario. Reiniciar o cancelar limpia los
valores y mensajes de validación anteriores.

`CrudModelTest` prueba los servicios simulados sin modificar datos reales:
eliminación de personas y roles, fallos, creación, edición, cancelación,
nombre vacío y reintento de creación cuando el servicio ya asignó un UUID.
Las operaciones persistentes completas deben verificarse con registros de prueba.

El pull de `origin/master` del 27 de septiembre de 2026 indicó que el checkout
ya estaba actualizado en `45fd052`; no hubo commits nuevos que integrar.
