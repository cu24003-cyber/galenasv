# Consulta y procedimientos

## Preparación

Aplicar `docs/migrations/20260930_consulta_procedimiento.sql` y después `docs/migrations/20261001_consulta_medico.sql` en PostgreSQL antes de desplegar `galenosv.war`. El proyecto tiene desactivada la creación automática de esquema. La primera migración valida las fechas y relaciones de procedimiento. La segunda agrega la autoría médica a consulta. No asigna un médico a consultas históricas porque no puede deducirse con seguridad de los pasos; un administrador debe verificar y completar `id_medico_rol` para que aparezcan en el historial.

Los pasos históricos sin definición conservan `id_procedimiento_paso` nulo: su correspondencia debe revisarse manualmente, pues el esquema anterior no permite deducirla. Las nuevas ejecuciones siempre guardan esa relación.

## Uso

1. En **Cambiar de rol**, seleccione una asignación existente de médico y clínica. El contexto se mantiene únicamente en la sesión HTTP; no hay tabla ni entidad de usuarios. El encabezado muestra la persona, el rol y la clínica actuales. Este selector no autentica al usuario: para garantizar privacidad entre personas reales se necesita integrar un inicio de sesión que vincule la identidad autenticada a la asignación médica.
2. En **Exámenes**, crear, editar, activar/desactivar o eliminar exámenes y seleccionar uno o varios tipos. El examen y sus clasificaciones se guardan juntos; un examen utilizado en pasos no se puede eliminar, pero se puede desactivar.
3. En **Procedimientos**, use las pestañas **Datos del procedimiento**, **Pasos y roles** y **Exámenes de los pasos** para crear el procedimiento y guardar; agregar pasos, rol responsable, paso anterior e indicador de fin. Se puede asociar un examen existente a cada paso. La secuencia guarda origen → destino; no permite agregar un sucesor a un paso final.
4. En **Pacientes**, seleccionar una fila y pulsar **Abrir consulta**. Solo se ofrecen asignaciones de paciente en la clínica del médico activo; el servidor valida también que ambas clínicas coincidan. Se guarda el médico creador. Solo él puede retomar y leer esa consulta desde esa clínica. El inicio original se conserva al recargar.
5. En la primera pestaña guardar referencia/observaciones, agregar procedimientos y completar sus pasos respetando dependencias. El inicio de consulta_procedimiento y sus pasos corresponde a la apertura de la consulta. `consulta.id_persona_rol` corresponde al paciente. En los pasos, `id_persona_rol` corresponde al responsable: inicialmente al rol activo de la sesión y posteriormente a la asignación seleccionada en la columna **Responsable**. Se admite cualquier rol activo de la clínica, sin limitarlo a Paciente ni al rol definido en el catálogo del paso. El rol del catálogo se muestra como referencia. Un paso completado conserva su responsable.
6. En **Registro de examen**, escoger un paso e ingresar nombre, tipo con autocompletado y observaciones. Se guardan conjuntamente examen, examen_tipo_examen y procedimiento_paso_examen. Estas tablas forman un catálogo compartido; los exámenes no son resultados clínicos individuales.
7. En **Orden de examen**, escoger el paso ejecutado e ingresar los exámenes solicitados e indicaciones. La orden queda vinculada al paso y su responsable, y al paciente mediante la consulta del procedimiento; orden_examen no tiene una columna id_examen.
8. En **Resultados de examen**, elegir una orden de la consulta e ingresar el resultado y, opcionalmente, la interpretación. También se pueden agregar resultados desde **Mis consultas** cuando lleguen después del cierre.
9. Completar los pasos y pulsar **Cerrar consulta**. Guarda los datos de consulta, su fin y el fin de sus procedimientos en una transacción. No modifica el inicio ni permite cambiar los datos clínicos cerrados, salvo agregar resultados a sus órdenes.

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
