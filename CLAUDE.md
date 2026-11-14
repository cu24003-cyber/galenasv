# galenasv (backend)

Backend de GalenoSV: Java 21, Jakarta EE 11, Open Liberty (puerto 9080), PostgreSQL en Docker. API en /galenosv/api. Patrón BCE.

## Prioridad absoluta
- Todo cambio debe cumplir exactamente las historias de usuario de ../ChivoMed/docs/rubrica.md. Si el código actual contradice la rúbrica, se cambia el código.
- HU1: la búsqueda de citas es pública, sin sesión.
- HU2: el login de empleados es con tipo de documento y número de documento, sin contraseña. Empleado = cualquier asignación con rol activo excepto Paciente (`rol.codigo = 'PACIENTE'`), en una clínica activa; un rol sin código también es empleado. Paciente, persona sin rol, rol inactivo o clínica inactiva: 401 `auth.credenciales`.
- HU3/HU4: cada paso lo atiende, completa, retrocede y le ordena exámenes solo quien tiene en esa clínica el rol responsable del paso (`procedimiento_paso.id_rol`); si no, 403 `consulta.noResponsablePaso`. Al completar un paso, su responsable pasa a ser quien lo completó. La consulta la cierra solo quien completó el paso de finalización (403 `consulta.soloFinalizador`). Detalle en docs/flujo-consulta.md y docs/api-rest.md ("Permisos por rol").

## Roles
- El Paciente se identifica siempre por `codigo = 'PACIENTE'`, nunca por nombre. Los pasos se comparan por `id_rol`.
- `ADMINISTRACION` se muestra como "Atención al público" y conserva su código: es el único rol que escribe catálogos, personas y /api/recepcion/*.
- Roles de la demo: Atención al público, Paciente, Enfermera, Doctor y Farmacéutico (inactivo).

## Datos demo
- Fuente: docs/datos-demo-rubrica.md (guion del Primer Parcial). Se cargan con `scripts/preparar-bd-desarrollo.sh` (docs/migrations/20261113_datos_demo_rubrica.sql y …_consultas.sql) y se comprueban con docs/migrations/tests/datos_demo_rubrica_test.sql.
- `scripts/reiniciar-demo.sh` devuelve las 11 consultas demo a su estado inicial.
- La demo anterior (admin.demo, doctor.demo, paciente.demo) se borra solo con docs/migrations/20261113_limpieza_demo_anterior.sql, después de un respaldo (docs/LEVANTAR.md §5.1).
- Los datos demo usan UUID fijos con prefijo `de12…`; todos son inventados.

## Reglas
- Solo se edita galenasv. ../ChivoMed es solo lectura.
- Cada endpoint nuevo o modificado lleva prueba JUnit y se documenta en docs/api-rest.md.
- No modificar el esquema sin una migración nueva en docs/migrations/.
- Endpoints bajo /api/publico/* no requieren sesión y nunca devuelven notas, valores clínicos ni resultados.
- No ejecutar docker ni borrar datos. Proponer plan antes de cambios en más de 3 archivos.
- Correr `mvn test` antes de dar una tarea por terminada e informar el resultado real.
- Rama de trabajo: backend-rubrica. Commits pequeños; no hacer force push ni push sin que se pida.
- Nunca poner datos reales de pacientes en código, pruebas ni migraciones.

- Los planes aprobados y sus reportes están en docs/reportes/ (el último es b8-plan.md / b8-reporte.md); no hagas brainstorming ni un plan nuevo salvo que se pida. Usa test-driven-development y verification-before-completion si están disponibles. No uses worktrees ni subagentes.
