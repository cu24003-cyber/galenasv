# Consulta y procedimientos

## Base compartida

El esquema de referencia es el proporcionado por el equipo. Su único parche pendiente está en `docs/migrations/consulta_procedimiento_paso.sql`: añade `id_procedimiento_paso`, `valor` y la clave foránea del paso ejecutado. La aplicación no genera ni modifica el esquema automáticamente. Aplique ese parche solamente mediante el proceso acordado por el equipo; no hay otros scripts de migración para este flujo.

La tabla `consulta` almacena el paciente en `id_persona_rol`. No tiene columna de médico creador. Por ello, el historial y la reanudación se limitan a la clínica del rol médico activo, sin atribuir consultas a una persona médica específica. El selector de rol de la aplicación no autentica usuarios.

Las consultas anteriores al parche pueden tener pasos sin `id_procedimiento_paso`. Se muestran en el historial, pero un paso sin definición no puede completarse automáticamente porque no es posible inferir su posición en la secuencia. Requiere que el equipo identifique manualmente el paso correcto.

## Flujo

1. En **Cambiar de rol**, seleccione una persona con rol Doctor o Médico y su clínica.
2. En **Personas**, asigne el rol Paciente a una persona de esa clínica.
3. En **Procedimientos**, configure pasos, responsables, secuencias y un paso final. Los roles responsables deben tener personas asignadas en la misma clínica.
4. En **Consultas**, seleccione al paciente y cree la consulta. La lista y el historial solo incluyen la clínica activa. Se puede retomar una consulta abierta desde el historial.
5. Pulse **Agregar procedimiento** para abrir el formulario, tanto en el catálogo como dentro de la consulta. Antes de iniciarlo, la aplicación verifica todos sus pasos, incluidos los posteriores: cada rol requerido debe estar activo y tener una persona distinta en la misma clínica. Cuatro roles diferentes requieren al menos cuatro personas que cubran esos roles; una persona con varios roles no cuenta varias veces. Si falta personal, se indica el motivo sin crear el procedimiento ni sus pasos. La aplicación crea los pasos iniciales y habilita los siguientes cuando se completan sus predecesores. Puede cambiar el responsable de un paso pendiente a otra persona con rol activo de la clínica. Al completarlo, puede guardar el campo `valor` del paso.
6. Registre exámenes, órdenes y resultados desde la consulta. Los resultados también pueden agregarse después del cierre desde el historial.
7. Complete los pasos finales y cierre la consulta. El historial permite filtrar por fecha de inicio y ver procedimientos, pasos, valores, órdenes y resultados. **Hasta** debe ser igual o posterior a **Desde**, incluso si se escribe la fecha manualmente; al mover **Desde** después de un **Hasta** elegido, se limpia el límite superior.

Las fechas se almacenan como `timestamp with time zone` y se muestran en la zona `America/El_Salvador`. La sesión HTTP conserva el rol y la consulta en curso. El filtro de navegación evita abandonar accidentalmente una atención abierta.

Un procedimiento solo se puede eliminar del catálogo si nunca se ha registrado en una consulta. La eliminación incluye sus pasos, secuencias y asociaciones con exámenes, conservando los catálogos de roles y exámenes.

En los formularios con **Cancelar**, el primer clic limpia los campos con datos sin guardarlos. Si los campos ya están vacíos, **Cancelar** oculta el editor; el botón para agregar un registro permite abrirlo de nuevo. Las listas de documentos, contactos, pasos y asociaciones permanecen visibles cuando se oculta su editor.

## Personas y documentos

Una persona se puede guardar sin rol, incluso si se ha seleccionado una clínica. La pertenencia a la clínica se conserva en `PersonaRol` con rol nulo y se recupera al editar; no habilita actuar como paciente o personal hasta asignar un rol. La clínica es necesaria cuando se asigna un rol. En la ficha de Personas se pueden eliminar documentos y medios de contacto sin guardar los borradores de otras pestañas. Los nombres y apellidos admiten letras Unicode, tildes, ñ y espacios; se rechazan números y símbolos tanto en JSF como al crear o actualizar desde el servicio. Una persona solo puede tener un DUI, identificado por el nombre `DUI` o `Documento Único de Identidad` del tipo de documento; esta regla también se comprueba al actualizar un documento. Los tipos de documento y de contacto no se pueden editar mientras existan registros que los referencien. Después de eliminar todas esas referencias se pueden modificar.

## Verificación

Compile con JDK 21 mediante `mvn package`. Las pruebas unitarias verifican las reglas del servicio y las vistas. La comprobación de integración con PostgreSQL debe hacerse en un entorno aislado que reproduzca el esquema compartido y el parche autorizado, sin escribir en la base compartida.
