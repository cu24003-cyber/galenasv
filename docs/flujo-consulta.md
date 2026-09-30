# Consulta y procedimientos

## Preparación

Aplicar `docs/migrations/20260930_consulta_procedimiento.sql` en PostgreSQL antes de desplegar `galenosv.war`. El proyecto tiene desactivada la creación automática de esquema. La migración valida que las fechas existentes sean `timestamp with time zone`, agrega la FK de consulta_procedimiento a procedimiento y el vínculo de cada paso ejecutado con su definición en procedimiento_paso. No convierte fechas antiguas suponiendo una zona horaria ni elimina registros huérfanos. Si encuentra inconsistencias, revierte toda la migración.

Los pasos históricos sin definición conservan `id_procedimiento_paso` nulo: su correspondencia debe revisarse manualmente, pues el esquema anterior no permite deducirla. Las nuevas ejecuciones siempre guardan esa relación.

## Uso

1. En **Procedimientos**, crear el procedimiento y guardar; agregar pasos, rol responsable, paso anterior e indicador de fin. Se puede asociar un examen existente a cada paso. La secuencia guarda origen → destino; no permite agregar un sucesor a un paso final.
2. En **Pacientes**, seleccionar una fila, elegir la clínica si hay varias asignaciones de paciente y pulsar **Abrir consulta**. Se guarda inmediatamente o se retoma una consulta abierta de esa asignación. El inicio original se conserva al recargar.
3. En la primera pestaña guardar referencia/observaciones, agregar procedimientos y completar sus pasos respetando dependencias. El inicio de consulta_procedimiento y sus pasos corresponde a la apertura de la consulta. `id_persona_rol` corresponde al paciente seleccionado.
4. En **Registro de examen**, escoger un paso e ingresar nombre, tipo con autocompletado y observaciones. Se guardan conjuntamente examen, examen_tipo_examen y procedimiento_paso_examen. Estas tablas forman un catálogo compartido; los exámenes no son resultados clínicos individuales.
5. En **Orden de examen**, escoger el paso ejecutado e ingresar los exámenes solicitados e indicaciones. La orden queda vinculada al paciente a través de ese paso, según el esquema existente; orden_examen no tiene una columna id_examen.
6. Completar los pasos y pulsar **Cerrar consulta**. Guarda los datos de consulta, su fin y el fin de sus procedimientos en una transacción. No modifica el inicio ni permite operaciones posteriores sobre una consulta cerrada.

Las fechas se almacenan con OffsetDateTime en UTC y se presentan en America/El_Salvador, incluyendo segundos y desplazamiento horario.

La navegación interna se bloquea en el servidor mediante ConsultaActivaFilter durante la atención. El cierre usa un envío normal de formulario y la navegación de Jakarta Faces, sin JavaScript personalizado ni callbacks. PrimeFaces conserva sus scripts internos para sus componentes. No se muestra una advertencia al cerrar la pestaña: sin código del navegador, el servidor no puede detectar ni impedir el cierre de pestaña, cierre del navegador o navegación externa. Una interrupción no cierra la consulta ni inventa una fecha de fin; puede retomarse seleccionando al mismo paciente y clínica. La expiración de la sesión tampoco cierra la consulta en la base.

## Validación

Compilar con JDK 21 mediante `mvn package`, sin ejecutar `clean` sobre el servidor local. Las pruebas del flujo cubren apertura/reanudación, rol de paciente, fechas, dependencias de pasos, cierre, pertenencia de órdenes y bloqueo de navegación. Verificar también el flujo completo sobre Liberty y PostgreSQL.

### Comprobación realizada

Se aplicó la migración a `clinica_sv` y se ejecutó el flujo completo en una instancia temporal de Liberty conectada al PostgreSQL de Docker: creación de procedimiento y secuencia, selección de paciente, apertura, guardado, examen con autocompletado, orden, rechazo de pasos fuera de secuencia, rechazo de cierre con pendientes, bloqueo de navegación y cierre correcto. PostgreSQL confirmó la igualdad de fechas entre consulta y procedimiento y el rol del paciente en los pasos. Los registros sintéticos se retiraron al finalizar. Se conservaron intactos los archivos `server.xml` del proyecto y del servidor habitual.
