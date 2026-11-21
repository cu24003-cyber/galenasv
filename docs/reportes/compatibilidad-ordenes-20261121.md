# Compatibilidad de órdenes — 2026-11-21

## Resultado

`OrdenExamen` dejó de mapear `orden_examen.id_examen`. Su UUID de examen y el texto se guardan en
`indicaciones`, columna text del esquema original. La API mantiene sus campos separados; las
vistas JSF también usan el texto decodificado. No se cambió el frontend, el esquema ni los permisos.
La consulta nombrada por indicaciones, sin consumidores, se retiró porque comparaba texto crudo.

El formato versionado reconoce solo UUID canónicos y un JSON completo con indicaciones string/null.
Texto legado, versiones desconocidas y contenido dañado se conservan sin inventar una asociación.
Texto de usuario que parece metadatos se codifica como texto literal. Las ediciones de indicaciones
conservan el examen, y el límite de usuario sigue siendo 255 caracteres.

Las escrituras de órdenes y los borrados de exámenes coordinan bloqueos sobre el mismo examen.
REST y JSF rechazan eliminar un examen que tiene órdenes, incluso sin asociaciones en el catálogo.

Se preparó `20261121_ordenes_base_original.sql` para conservar las asociaciones de bases propias
con la columna antigua. Es transaccional e idempotente; no agrega ni elimina columnas. El arranque
comprueba esas bases y rechaza el despliegue si quedan órdenes con asociaciones sin convertir.
Los fixtures de desarrollo se convierten después de cargarse; no se editaron migraciones históricas.
Instrucciones: [conversion-ordenes-base-original.md](../conversion-ordenes-base-original.md).

## Verificaciones

- `mvn -q test`: 815 pruebas, cero fallos, cero errores, cero omitidas.
- WAR generado con `mvn -q package` y ejecutado en Open Liberty 26.0.0.11 / PostgreSQL 18.3.
- API real con original + parche: creación de órdenes de varios exámenes, listado y UUID usados
  por «Ya ordenado», lecturas de texto legado/dañado, resultados, rechazo de un rol no responsable
  y conservación del rechazo de edición directa. La edición del recurso sin perder el examen se
  prueba además con JUnit/Jersey; no se ampliaron permisos para habilitarla en producción.
- Reinicio real: examen, indicaciones y resultados permanecen vinculados a sus órdenes.
- Segunda copia temporal con columna antigua: conversión conserva UUID/texto/null, repetición no
  modifica filas ya convertidas y un formato dañado revierte toda la transacción.
- Comprobación de arranque real: una orden pendiente provoca fallo de despliegue y HTTP 404;
  después de convertir la copia, la aplicación arranca y devuelve sus asociaciones correctamente.
- Concurrencia real: una transacción retiene el bloqueo de examen y registra una orden; un DELETE
  HTTP simultáneo espera y luego devuelve 409 `error.relacionado`, conservando el examen. Este
  examen de prueba no tenía asociaciones de catálogo ni FK de orden que pudieran ocultar el fallo.
- Completar paso y cerrar consulta: se rechazan nuevas órdenes y se admiten resultados posteriores.
- Cuatro archivos de pruebas existentes del frontend pasaron: controlador de consulta, acciones,
  servicios y paneles. No se modificaron archivos del frontend.
- `bash -n` de los dos scripts de desarrollo y `git diff --check`, correctos.
- Dump de esquema antes/después en la base original: idéntico tras quitar los tokens aleatorios
  restrict/unrestrict de pg_dump. SHA-256 normalizado:
  `528893f4dd944ac0986bd54f863baf5fe44c2f631ae7249b8749e73a322d0b60`.

Bases de prueba: `galeno_original` y `galeno_conversion`, puerto 55432; Liberty separado en 19080.
Todo usó datos ficticios. Archivos temporales de prueba en `/tmp/galeno-ordenes` y `/tmp/galeno-compat`.
No se ejecutó Docker ni la conversión sobre la base actual o una base del ingeniero.

## Pendientes

Antes de desplegar sobre la base propia: respaldo validado y ejecución de la conversión, con
el backend detenido. La columna antigua queda histórica; no volver al WAR anterior sobre esa
base convertida. La base original del ingeniero no necesita esa conversión ni columnas nuevas.

Aún falta controlar documentos duplicados al guardar (incluida concurrencia), terminar de revisar
referencias sin las FK adicionales, recorrer HU1–HU4 completos desde el navegador y preparar
POM/provided, bibliotecas externas, contexto/proxy y Docker. No se verificó aquí el empaquetado final.

Rama de trabajo: `backend-rubrica`. Identidad Git conservada:
Julio Ernesto Calderon Umana <cu24003@ues.edu.sv>.
