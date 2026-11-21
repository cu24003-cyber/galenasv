# Órdenes compatibles con la base original + parche

El backend ya no necesita `orden_examen.id_examen`. El examen elegido y las indicaciones se
almacenan en la columna `indicaciones` (text) que existe en el DDL original. La API y las vistas
siguen mostrando el UUID y el texto por separado. Los resultados conservan su vínculo con la orden.

## Base del ingeniero

Aplicar únicamente su DDL original y su parche. No ejecutar nuestras migraciones de desarrollo,
no cargar datos demo y no agregar `rol.codigo` ni `orden_examen.id_examen`. No hay conversión que
hacer si esa columna adicional nunca existió. Las órdenes con solo texto siguen legibles, sin
asignarles un examen por su nombre.

## Base propia con la columna adicional antigua

Antes de desplegar esta versión:

1. Detener el backend y cualquier otro escritor de esa base.
2. Hacer un respaldo completo y verificarlo. Ejemplo, sustituyendo los valores de conexión:

   ```bash
   pg_dump -h HOST -p PUERTO -U USUARIO -d BASE -Fc -f respaldo-antes-ordenes.dump
   pg_restore --list respaldo-antes-ordenes.dump
   ```

   `pg_restore --list` comprueba que el archivo se puede leer; probar su restauración en una base
   separada antes de usarlo como recuperación. No subir el respaldo ni credenciales a Git.

3. Probar primero en una copia y luego ejecutar, desde la raíz del backend, la conversión:

   ```bash
   psql -X -v ON_ERROR_STOP=1 -h HOST -p PUERTO -U USUARIO -d BASE \
     -f docs/migrations/20261121_ordenes_base_original.sql
   ```

4. Desplegar el WAR nuevo, consultar órdenes existentes y comprobar examen, indicaciones y resultados.
   Si el arranque indica asociaciones pendientes, revisar la conversión; no ignorar la comprobación.

La conversión usa una transacción y bloquea escrituras en `orden_examen`. Conserva UUID de órdenes,
UUID de exámenes, fechas, indicaciones (incluido null) y resultados. No elimina la columna antigua:
queda como referencia histórica y el backend nuevo deja de leerla/escribirla. El contenido
estructurado pasa a ser la fuente vigente; una edición posterior no sincroniza la columna antigua.

Puede repetirse sin volver a envolver órdenes ya convertidas. Un prefijo reservado desconocido,
un UUID inválido o JSON dañado produce un error y revierte toda la conversión. Revisar esos casos
en la copia; no corregirlos adivinando el examen. El script no modifica órdenes con `id_examen` null.

No volver a ejecutar un WAR antiguo contra la base convertida: mostraría el formato interno y
seguiría usando la columna histórica. Para revertir, utilizar el respaldo validado en una base
separada y evaluar las escrituras realizadas después de la conversión.

Los scripts `preparar-bd-desarrollo.sh` y `reiniciar-demo.sh` convierten las órdenes que generan
sus fixtures históricos. Son herramientas de desarrollo, no de preparación de la base del ingeniero.
Ejecutarlos siempre con el backend detenido. Las migraciones históricas no se editaron.

## Formato y garantías

Formato interno v1: `@galenosv/orden/v1:<UUID o ->:{"indicaciones":<texto o null>}`.
El guion representa una orden sin examen identificado. El prefijo es reservado. El texto que
escriba un usuario se escapa dentro del JSON, incluso si parece ese formato, para que no pueda
cambiar la asociación con su contenido.

El lector solo reconoce un UUID canónico y un objeto JSON completo con una propiedad
`indicaciones` de tipo string/null. Texto legado o contenido inválido se conserva íntegro, sin
inventar una relación. Editar las indicaciones mediante los métodos existentes conserva el UUID;
se mantiene el límite de 255 caracteres de usuario y los permisos actuales de la aplicación.

Sin una FK adicional, el backend coordina creación/edición de órdenes y eliminación de exámenes
con un bloqueo sobre la fila del examen. Rechaza borrar exámenes con órdenes; se pueden desactivar.
Estas protecciones aplican a las escrituras de la aplicación, no a SQL externo que las omita.

Esta conversión se comprobó solo en bases temporales con datos ficticios. No se ejecutó sobre
la base actual del proyecto ni sobre una base del ingeniero.
