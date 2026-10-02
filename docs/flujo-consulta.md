# Consulta y procedimientos

## Preparación

Aplicar `docs/migrations/20260930_consulta_procedimiento.sql` y después `docs/migrations/20261001_consulta_medico.sql` en PostgreSQL antes de desplegar `galenosv.war`. El proyecto tiene desactivada la creación automática de esquema. La primera migración valida las fechas y relaciones de procedimiento. La segunda agrega la autoría médica a consulta. No asigna un médico a consultas históricas porque no puede deducirse con seguridad de los pasos; un administrador debe verificar y completar `id_medico_rol` para que aparezcan en el historial.

Los pasos históricos sin definición conservan `id_procedimiento_paso` nulo: su correspondencia debe revisarse manualmente, pues el esquema anterior no permite deducirla. Las nuevas ejecuciones siempre guardan esa relación.

## Uso

1. En **Cambiar de rol**, seleccione primero una clínica y luego una persona de esa clínica. El campo de rol está desactivado y muestra automáticamente su asignación; conserva el rol de la sesión si corresponde a esa persona y clínica, o utiliza la primera asignación activa disponible. Al cambiar de clínica o persona se descarta la selección anterior. El contexto se mantiene únicamente en la sesión HTTP; no hay tabla ni entidad de usuarios. El encabezado muestra la persona, el rol y la clínica actuales. Este selector no autentica al usuario: para garantizar privacidad entre personas reales se necesita integrar un inicio de sesión que vincule la identidad autenticada a la asignación médica.
2. En **Exámenes**, crear, editar, activar/desactivar o eliminar exámenes y seleccionar uno o varios tipos activos. Al crear un examen no se muestran tipos inactivos; al editar se conservan las clasificaciones inactivas que ya tenía asignadas. El examen y sus clasificaciones se guardan juntos; un examen utilizado en pasos no se puede eliminar, pero se puede desactivar.
3. En **Procedimientos**, use las pestañas **Datos del procedimiento**, **Pasos y roles** y **Exámenes de los pasos** para crear el procedimiento y guardar; agregar pasos, rol responsable, paso anterior e indicador de fin. Se puede asociar un examen existente a cada paso. La secuencia guarda origen → destino; no permite agregar un sucesor a un paso final.
4. En **Consultas**, seleccionar un paciente de la clínica del médico activo, ingresar opcionalmente referencia externa y observaciones, y pulsar **Crear consulta**. El paciente, el médico creador, el inicio y los datos se guardan en una transacción; el fin se registra al cerrar. El servidor valida ambas clínicas y rechaza una nueva consulta si el paciente ya tiene una abierta. El historial de esta misma página solo muestra consultas del médico y la clínica en sesión. Los filtros Desde y Hasta incluyen todo el día local en America/El_Salvador; el campo Buscar por nombre reduce la lista de pacientes de la clínica sin quitar el selector. **Retomar consulta** permite continuar una consulta propia abierta sin cambiar su inicio ni sus datos. **Pacientes** también limita su listado a la clínica activa y mantiene la opción de abrir o retomar consultas.
5. En la primera pestaña guardar referencia/observaciones, agregar procedimientos y completar sus pasos respetando dependencias. El inicio de consulta_procedimiento y sus pasos corresponde a la apertura de la consulta. `consulta.id_persona_rol` corresponde al paciente. En los pasos, `id_persona_rol` corresponde al responsable. Al agregar un procedimiento se crean únicamente los pasos iniciales (sin predecesor), asignados a una persona con el rol activo indicado por el catálogo en la clínica de la consulta. Si falta un responsable, la operación falla y se revierte. Al completar un paso, se crean los sucesores solo cuando todos sus predecesores están completos, con la misma selección automática por rol. La columna **Responsable** permite cambiar manualmente la asignación a cualquier rol activo de la clínica. Un paso completado conserva su responsable. El cierre exige que cada procedimiento haya alcanzado un paso marcado como final.
6. En **Registro de examen**, escoger un paso e ingresar nombre, tipo con autocompletado y observaciones. Se guardan conjuntamente examen, examen_tipo_examen y procedimiento_paso_examen. Estas tablas forman un catálogo compartido; los exámenes no son resultados clínicos individuales.
7. En **Orden de examen**, escoger el paso ejecutado e ingresar los exámenes solicitados e indicaciones. La orden queda vinculada al paso y su responsable, y al paciente mediante la consulta del procedimiento; orden_examen no tiene una columna id_examen.
8. En **Resultados de examen**, elegir una orden de la consulta e ingresar el resultado y, opcionalmente, la interpretación. También se pueden agregar resultados desde **Consultas** cuando lleguen después del cierre.
9. Completar los pasos y pulsar **Cerrar consulta** para regresar a **Consultas**. Guarda los datos de consulta, su fin y el fin de sus procedimientos en una transacción. No modifica el inicio ni permite cambiar los datos clínicos cerrados, salvo agregar resultados a sus órdenes.

Las fechas se almacenan con OffsetDateTime en UTC y se presentan en America/El_Salvador, incluyendo segundos y desplazamiento horario.

La navegación interna se bloquea en el servidor mediante ConsultaActivaFilter durante la atención, excepto **Cambiar de rol**. El cambio a otra asignación se rechaza mientras la consulta esté abierta. El cierre usa un envío normal de formulario y la navegación de Jakarta Faces. Una interrupción no cierra la consulta ni inventa una fecha de fin; puede retomarse seleccionando al mismo paciente con la misma asignación médica y clínica. La expiración de la sesión tampoco cierra la consulta en la base.

## Validación

Compilar con JDK 21 mediante `mvn package`, sin ejecutar `clean` sobre el servidor local. Las pruebas del flujo cubren apertura/reanudación, clínica del responsable, asignación de cualquier rol activo, sesión aislada y serializable, clasificación de exámenes, fechas, dependencias de pasos, cierre, pertenencia de órdenes y bloqueo de navegación con excepción del cambio de rol. Verificar también el flujo completo sobre Liberty y PostgreSQL.

### Comprobación histórica anterior al cambio de rol

Se aplicó la migración a `clinica_sv` y se ejecutó el flujo completo en una instancia temporal de Liberty conectada al PostgreSQL de Docker: creación de procedimiento y secuencia, selección de paciente, apertura, guardado, examen con autocompletado, orden, rechazo de pasos fuera de secuencia, rechazo de cierre con pendientes, bloqueo de navegación y cierre correcto. PostgreSQL confirmó la igualdad de fechas entre consulta y procedimiento y el rol del paciente en los pasos. Los registros sintéticos se retiraron al finalizar. Se conservaron intactos los archivos `server.xml` del proyecto y del servidor habitual.

### Validación del cambio de rol y catálogo de exámenes

Se generó `target/galenosv.war` con JDK 21 y pasaron 39 pruebas de atención, sesión, navegación y exámenes. Se verificaron en una instancia temporal de Liberty el formulario de exámenes y sus campos obligatorios, la selección de tipos, las pestañas del catálogo de procedimientos, el cambio de rol y la selección de clínica para un paciente. Estas comprobaciones de vistas no crearon consultas ni modificaron registros de la base.

La suite completa presenta cuatro fallos previos en `PersonaModelTest`, `CrudModelTest` e `I18nTest`; se reprodujeron también en una copia del commit original, sin estos cambios. En este entorno se ejecutó Mockito como agente explícito porque la auto-conexión del agente está restringida por el sandbox.

### Validación del historial y resultados (2026-10-01)

Se agregaron pruebas de autoría médica, clínica, bloqueo de cambio de identidad y pertenencia de órdenes. Se corrigieron los cuatro fallos previos de carga, eliminación y textos. El WAR compila y pasan 264 pruebas con Mockito cargado como agente. Las vistas XHTML se comprobaron como XML válido.

El 2026-10-01 se creó una copia previa de la base local `galenasv` en `/tmp/galenasv-before-consulta-migrations.dump` y se aplicaron ambas migraciones en orden. Se verificaron `id_procedimiento_paso`, `id_medico_rol` y la FK médica. Liberty arrancó conectado a PostgreSQL; **Pacientes**, **Consulta**, **Mis consultas** y **Cambiar de rol** respondieron HTTP 200 sin errores en el registro. No se creó una consulta de prueba porque la base no tenía asignaciones de persona, rol y clínica; el flujo completo de escritura sobre la base queda sin comprobar.

### Registro de personas

Una persona puede guardarse sin rol ni clínica y luego registrar sus documentos y medios de contacto. Para crear una asignación de `persona_rol` deben seleccionarse tanto el rol como la clínica activos. La opción **Nueva asignación** permite agregar una asignación a una persona ya guardada. Guardar solamente los datos personales no elimina asignaciones existentes, pues pueden estar referenciadas por consultas y pasos.

### Validación de creación de consultas y selección por clínica (2026-10-01)

El WAR compila con JDK 21 y pasan 286 pruebas. Se agregaron verificaciones de creación transaccional con autor, clínica, límites de texto, rechazo de duplicados, recuperación de consultas, personas sin rol, filtrado de tipos inactivos y selección dependiente de clínica/persona/rol. El historial descarta los datos de un contexto anterior si cambia el rol desde otra pestaña. Todas las vistas XHTML son XML válido. La ruta anterior `historial-consultas.xhtml` conserva compatibilidad y presenta la nueva página **Consultas**.

Se comprobó por HTTP en Liberty con un esquema temporal que copió las 20 tablas y sus 22 claves foráneas, sin copiar datos reales. Se verificaron la selección dependiente de clínica/persona/rol, pacientes de la clínica, creación de consulta, bloqueo de navegación, procedimientos, asignación a personal de la clínica, examen y clasificación, orden, resultado, cierre, detalle y compatibilidad de la ruta anterior. También se comprobaron la reanudación desde otra sesión sin sobrescribir datos, rechazo de consultas duplicadas, bloqueo de cambio de identidad durante la atención, historial vacío para otro médico de la misma clínica y para otra clínica, persona creada sin rol y ocultamiento de tipos inactivos al crear exámenes.

La base local `clinica_sv` todavía carecía de `consulta.id_medico_rol`. Tras validar ambas migraciones en el esquema temporal, se preparó y verificó el respaldo de `public` en `/tmp/clinica_sv-before-consultas-20261001.dump` y se aplicaron las migraciones existentes en orden a la base local. No se asignó autoría a registros antiguos; la comprobación posterior encontró cero consultas históricas sin autor. Las pruebas de escritura se realizaron únicamente en el esquema temporal, que se eliminó al finalizar, junto con el servidor de prueba.

## Instalación desde cero

Consulte `docs/README-ejecucion.md`. `docs/schema.sql` contiene las 20 tablas para una base vacía y ya incorpora las dos migraciones. `src/main/liberty/config/server.xml.example` contiene únicamente valores de ejemplo.

### Verificación aislada de la rúbrica (2026-10-01)

Se creó `codex_rubric_20261002` en PostgreSQL sin copiar filas de `public`; `docs/schema.sql` produjo 20 tablas y 24 claves foráneas. En una instancia temporal de Liberty conectada a ese esquema se crearon desde las pantallas tres clínicas, cinco roles, tres tipos de contacto, tres tipos de documento, tres tipos de examen, tres exámenes, dos procedimientos y cinco personas. Los selectores de tipos y roles omitieron los registros inactivos; un DUI y un correo con formato incorrecto mostraron el error de validación. En edición, el selector de tipos de examen y los selectores de rol y clínica de una asignación quedaron deshabilitados; el examen se guardó con su clasificación conservada.

En Consultas, solo apareció el paciente con rol Paciente de la clínica del médico. La búsqueda por nombre redujo el selector. Agregar el procedimiento de tres pasos creó solo Recepción para Atención al público; completar Recepción creó Evaluación para Enfermera, y completarla creó Diagnóstico para Doctor. Se rechazó cerrar con pasos pendientes, se cerró tras completar el paso final y el historial del mismo día incluyó la consulta. Un rango Desde posterior a Hasta mostró el mensaje de error. La instancia temporal y el esquema se eliminaron al terminar; no se modificó `public`.
