# Compatibilidad con base original: primer bloque, 2026-11-21

## Resultado

Roles, login, citas públicas, dashboard y recepción se comprobaron contra una base temporal creada
únicamente con el DDL original de Classroom y su parche. La aplicación ya no mapea ni consulta
`rol.codigo`; sigue entregando `codigo` y `codigoRol` en JSON. No se modificó el frontend.

Decisión del usuario: usar los nombres Paciente y Atención al público/Administración, conservando
la clasificación al editar. Se aceptan mayúsculas y espacios exteriores, con las tildes acordadas.
Doctor, Enfermera y Farmacéutico conservan sus códigos de API; otros nombres devuelven null.
Los responsables de pasos se siguen comparando por UUID.

La edición REST y JSF rechaza cambios entre paciente, administración y empleado con
`rol.nombreReservado`. Permite alternar los dos nombres administrativos y renombrar empleados
sin convertirlos en roles especiales. Crear un rol especial exige usar el nombre acordado.

El login rechaza un tipo/número vinculado a varias personas. HU1 tampoco devuelve sus clínicas,
citas ni tickets; no escoge una persona arbitrariamente. Esto protege las lecturas aunque la
base original no tenga nuestra restricción UNIQUE adicional.

## Validación realizada

- `mvn test` y posteriormente `mvn -q package`: 803 pruebas, cero fallos, cero errores, cero omitidas.
- WAR generado y ejecutado en Open Liberty 26.0.0.11 con PostgreSQL 18.3 temporal.
- 29 comprobaciones HTTP con personas ficticias: paciente/inactivo/documento ambiguo rechazados;
  login de Doctor, Laboratorista y Administración; contrato de sesión y asignaciones; dashboard;
  búsqueda pública; protección de renombrado; cambio válido a Administración; alta de paciente
  desde recepción y alta de rol sin columna adicional.
- La prueba real encontró una referencia SQL fuera de alcance entre subconsultas. La consulta
  ahora une explícitamente la persona en el nivel exterior; las comprobaciones se repitieron y pasaron.
- Comparación de `pg_dump --schema-only` antes/después: idéntica, exceptuando los tokens aleatorios
  `restrict/unrestrict` del propio dump. SHA-256 normalizado:
  `528893f4dd944ac0986bd54f863baf5fe44c2f631ae7249b8749e73a322d0b60`.
- Se confirmó que ni `rol.codigo` ni `orden_examen.id_examen` existen en esa base de prueba.
- `git diff --check` sin errores y frontend sin modificaciones.

La prueba usó puertos separados: PostgreSQL 55432 y Liberty 19080. Datos y logs temporales en
`/tmp/galeno-compat`; no se instalaron servicios del sistema, no se ejecutó Docker ni se modificó
la base actual del proyecto. No se ejecutaron las migraciones adicionales de desarrollo.

## Alcance pendiente

Este resultado no certifica todavía todo HU3/HU4 contra la base del ingeniero. Falta adaptar las
órdenes para dejar de requerir `orden_examen.id_examen`, validar su persistencia y resultados,
controlar duplicados de documentos al guardar (incluida concurrencia), revisar las referencias
que antes dependían de FK adicionales y hacer el recorrido completo desde el navegador.

Después corresponde empaquetar: POM/provided, bibliotecas externas de Liberty, contexto/proxy
y Docker. La suite pasó con el POM actual; todavía no se probó con dependencias provided.
No se consultaron datos reales del ingeniero; sus roles deben respetar los nombres acordados.
El código antiguo almacenado en nuestra base deja de definir permisos: nombres fuera de la
convención no se reconocen como paciente/administración. No se cambiaron nombres ni datos existentes.

Rama de trabajo: `backend-rubrica`. Identidad Git verificada:
Julio Ernesto Calderon Umana <cu24003@ues.edu.sv>.
