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
5. Agregue un procedimiento. La aplicación crea los pasos iniciales y habilita los siguientes cuando se completan sus predecesores. Puede cambiar el responsable de un paso pendiente a otra persona con rol activo de la clínica. Al completarlo, puede guardar el campo `valor` del paso.
6. Registre exámenes, órdenes y resultados desde la consulta. Los resultados también pueden agregarse después del cierre desde el historial.
7. Complete los pasos finales y cierre la consulta. El historial permite filtrar por fecha de inicio y ver procedimientos, pasos, valores, órdenes y resultados.

Las fechas se almacenan como `timestamp with time zone` y se muestran en la zona `America/El_Salvador`. La sesión HTTP conserva el rol y la consulta en curso. El filtro de navegación evita abandonar accidentalmente una atención abierta.

## Verificación

Compile con JDK 21 mediante `mvn package`. Las pruebas unitarias verifican las reglas del servicio y las vistas. La comprobación de integración con PostgreSQL debe hacerse en un entorno aislado que reproduzca el esquema compartido y el parche autorizado, sin escribir en la base compartida.
