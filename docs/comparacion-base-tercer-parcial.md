# Comparación de la base de Classroom con el esquema del proyecto

Revisión: 2026-11-20. Backend revisado: `/home/junior/galenosv`, commit `cda6ce6`.
No se ejecutó SQL de los adjuntos, migraciones, scripts de preparación ni Docker.
No se modificaron esquemas ni datos.

## Alcance y límite de la comprobación

Se extrajo únicamente el SQL de las etiquetas `<pre>` de las capturas MHTML:

- `/home/junior/Descargas/Clinica DDL - Classroom.mhtml`: `clinica_ppi115_2026_08_20.sql`.
- `/home/junior/Descargas/Clinica DDL - Classroomparche.mhtml`: contiene la misma base original y, además, `clinica_ppi115_2026_09_29_patch_consultaProcedimientoPaso.sql`. La base embebida en ambos archivos es idéntica.
- Esquema del proyecto: secuencia de migraciones que referencia [preparar-bd-desarrollo.sh](../scripts/preparar-bd-desarrollo.sh), leída sin ejecutarla.
- Referencia adicional: `/home/junior/Documentos/postgres/galenasv.sql`. Sus 61 sentencias de tablas, restricciones e índices son equivalentes a las de la base original al normalizar espacios. Es un respaldo antiguo, no evidencia del esquema activo.

Se intentó consultar solo los catálogos de PostgreSQL con `default_transaction_read_only=on`, sin consultar registros de pacientes. La conexión a `localhost:5432`, destino del `server.xml` local, fue rechazada. Por tanto, **“proyecto” significa el esquema esperado al aplicar sus migraciones**, no una confirmación de cuáles se aplicaron en la instancia real. No se arrancó ningún servicio.

## Original → original con parche

La base original define 20 tablas, 99 columnas, 20 claves primarias, 20 claves foráneas y un índice adicional sobre `persona_rol.id_clinica`. El parche conserva las 20 tablas y agrega:

| Cambio en `consulta_procedimiento_paso` | Efecto |
| --- | --- |
| `id_procedimiento_paso uuid` | Identifica el paso del catálogo que se atendió; admite NULL. |
| `valor text` | Guarda lo registrado en el paso; admite NULL. |
| FK `fk_consulta_procedimiento_paso_consulta_paso` | Enlaza `id_procedimiento_paso` con `procedimiento_paso.id_procedimiento_paso`; `ON UPDATE CASCADE`, `ON DELETE RESTRICT`, `NOT VALID`. |

`NOT VALID` evita comprobar las filas anteriores al crear la FK, pero las inserciones y actualizaciones nuevas sí deben cumplirla. El parche no rellena las columnas nuevas ni cambia datos existentes mediante INSERT/UPDATE/DELETE. No añade índices, tablas ni valores predeterminados.

El mismo parche ya está en [consulta_procedimiento_paso.sql](migrations/consulta_procedimiento_paso.sql), y sus columnas/FK también están incorporadas en la migración de base completa. **No se debe aplicar ciegamente el parche encima del proyecto**: sus `ADD COLUMN` y `ADD CONSTRAINT` no comprueban si esos objetos existen; `IF EXISTS` solo comprueba la tabla.

## Original con parche → esquema esperado del proyecto

| Objeto | Diferencia adicional | Fuente |
| --- | --- | --- |
| `rol.codigo` | Columna `varchar(20)` nullable e índice único `uq_rol_codigo`; códigos estables para permisos. Se permiten varios NULL. | `20261111_base_completa.sql` |
| `documento` | Restricción `uq_documento_tipo_valor UNIQUE (id_tipo_documento, valor)`; evita repetir el mismo documento dentro de un tipo. Comprueba duplicados antes de crearla. | `20261112_documento_tipo_valor_unico.sql` |
| `orden_examen.id_examen` | Columna `uuid` nullable, FK `fk_orden_examen_examen` hacia `examen`, CASCADE al actualizar y RESTRICT al borrar, e índice `ix_orden_examen_examen`. Las órdenes antiguas pueden conservar NULL; la API exige un examen al crear órdenes nuevas. | `20261113_orden_examen_examen.sql` |
| `consulta_procedimiento.id_procedimiento` | FK `fk_consulta_procedimiento_procedimiento` hacia `procedimiento`, ausente del DDL original. | `20261111_base_completa.sql` |
| `procedimiento_paso_secuencia.id_procedimiento_paso_referencia` | FK `fk_procedimiento_paso_secuencia_referencia` hacia `procedimiento_paso`, ausente del DDL original. | `20261111_base_completa.sql` |
| Validación de FK | La base completa valida las FK de su lista, incluidas las que el original/parche dejaban `NOT VALID`: `consulta_persona_rol`, `fk_procedimiento_paso_rol` y la FK nueva del parche. Puede fallar si existen referencias huérfanas. | `20261111_base_completa.sql` |
| `usuario` | La base completa crea una tabla histórica de cuentas con restricciones e índices; `20261115_login_por_documento.sql` la elimina. No pertenece al esquema final esperado. | Ambas migraciones |

Las dos FK adicionales de la base completa también usan `ON UPDATE CASCADE` y `ON DELETE RESTRICT`. No cambian los tipos, longitudes, valores predeterminados ni nulabilidad de las 99 columnas originales en las declaraciones de las 20 tablas clínicas.

Partiendo exactamente del DDL original y completando la secuencia del proyecto, el resultado esperado es **20 tablas clínicas, 103 columnas, 20 PK, 24 FK y la restricción única de documento**. Las cuatro columnas añadidas frente al original son las dos del parche, `rol.codigo` y `orden_examen.id_examen`. La tabla `usuario`, sus índices y sus restricciones se eliminan al final; no se cuentan en ese resultado.

Esto describe las declaraciones y migraciones, no una reconciliación automática de cualquier base divergente: `CREATE TABLE IF NOT EXISTS` no corrige por sí solo columnas existentes de tipos distintos.

## Datos, migraciones históricas y compatibilidad

Los SQL adjuntos contienen estructura, sin filas INSERT/COPY de datos. El proyecto además incluye datos de demostración: catálogos, roles, clínicas, personas ficticias, documentos, contactos, asignaciones y 11 consultas con pasos, órdenes y resultados. No se comprobó qué filas existen en la instancia real.

La base completa puede asignar códigos a roles existentes o crear los roles canónicos que falten. Los scripts demo contienen INSERT/UPDATE y el de consultas también DELETE de registros de esa demo para reconstruirla. La limpieza de la demo anterior es otro script separado. Ninguno se ejecutó durante esta revisión.

La tabla `usuario` es un cambio intermedio, no una tabla que deba añadirse para usar el login actual. La migración del 15 de noviembre la elimina junto con las cuentas que contenga; no elimina `persona`, `documento` ni `persona_rol`.

**La base original más el parche no basta para el backend actual**: todavía faltarían `rol.codigo`, el vínculo de cada orden con su examen y las restricciones adicionales. Volver a esa estructura podría romper consultas y persistencia de la aplicación. No se propone ni se aplica una migración en esta tarea.
