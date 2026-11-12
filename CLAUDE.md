# galenasv (backend)

Backend de GalenoSV: Java 21, Jakarta EE 11, Open Liberty (puerto 9080), PostgreSQL en Docker. API en /galenosv/api. Patrón BCE.

## Prioridad absoluta
- Todo cambio debe cumplir exactamente las historias de usuario de ../ChivoMed/docs/rubrica.md. Si el código actual contradice la rúbrica, se cambia el código.
- HU2: el login de empleados es con tipo de documento y número de documento, sin contraseña. Solo empleados (DOCTOR, ADMINISTRACION) se autentican.
- HU1: la búsqueda de citas es pública, sin sesión.

## Reglas
- Solo se edita galenasv. ../ChivoMed es solo lectura.
- Cada endpoint nuevo o modificado lleva prueba JUnit y se documenta en docs/api-rest.md.
- No modificar el esquema sin una migración nueva en docs/migrations/.
- Endpoints bajo /api/publico/* no requieren sesión y nunca devuelven notas, valores clínicos ni resultados.
- No ejecutar docker ni borrar datos. Proponer plan antes de cambios en más de 3 archivos.
- Correr `mvn test` antes de dar una tarea por terminada e informar el resultado real.
- Rama de trabajo: backend-rubrica. Commits pequeños; no hacer force push.
- Nunca poner datos reales de pacientes en código, pruebas ni migraciones.

- Ya existe un plan aprobado; no hagas brainstorming ni un plan nuevo salvo que se pida. Usa test-driven-development y verification-before-completion si están disponibles. No uses worktrees ni subagentes.
