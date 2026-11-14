# B8: regla de roles por paso, datos demo del guion y pendientes de la B6

## Contexto

`docs/datos-demo-rubrica.md` reemplaza la demo actual (Clínica de demostración, admin.demo, doctor.demo y paciente.demo) por la del guion del Primer Parcial: 3 clínicas, 5 roles y procedimientos donde cada paso lo atiende un rol distinto (Enfermera → Doctor → Atención al público). Con la regla de hoy, esa demo no funciona:

- solo DOCTOR y ADMINISTRACION inician sesión;
- solo DOCTOR toca la atención clínica.

Lucía (Enfermera) no podría entrar, y Marta (Atención al público) no podría completar el último paso. La rúbrica (HU2, HU3 y HU4) habla de "empleado" en general y del "rol y clínica" que eligió. Por eso la regla pasa a ser:

- **Empleado:** cualquier rol activo excepto Paciente, en una clínica activa.
- **Paso:** solo lo atienden, completan, retroceden o le ordenan exámenes quienes tienen en esa clínica el rol responsable del paso.
- **Cierre:** lo hace quien completó el paso de finalización.

Decisiones que ya tomaste: ADMINISTRACION se **renombra** a "Atención al público". Los datos demo viejos se borran con un **script de limpieza aparte**, que ejecutas tú.

## Cómo se identifican hoy los roles

Se mezclan dos formas:

- **Por `rol.codigo`** (único, varchar 20, sembrado por `20261111_base_completa.sql` con ADMINISTRACION, DOCTOR y PACIENTE): `UsuarioRepository.asignaciones` (`codigo IN ('ADMINISTRACION','DOCTOR')`), `AutenticacionService.ROLES_EMPLEADO`, `PermisosApiFilter` (líneas 44 a 163), `RecepcionResource:37-38`, `ConsultaResource:51`, `CitaPublicaRepository:20` y `AtencionService:53,62`.
- **Por nombre**: `AtencionService:29,86` (`lower(nombre)='paciente'`), `AtencionService:47-54` (respaldo por nombre "medico" o "doctor" cuando el código es null), `PersonaRolRepository:23` y `CatalogoInicialService:21`.
- **Por `id_rol`** (como debe ser): el rol responsable de cada paso (`procedimiento_paso.id_rol`), `asignarResponsable`, `responsableAutomatico` y `pendienteParaMi`.

**Cómo quedan los 5 roles del archivo.** El código solo se usa para lo que es especial: Paciente (no es empleado) y Atención al público (recepción y catálogos). Todo lo de los pasos va por `id_rol`.

| Rol del archivo | Fila en `rol` | `codigo` | Activo |
|---|---|---|---|
| Atención al público | la de ADMINISTRACION (mismo UUID), con el nombre cambiado | `ADMINISTRACION` | sí |
| Paciente | la de PACIENTE | `PACIENTE` | sí |
| Doctor | la de DOCTOR | `DOCTOR` | sí |
| Enfermera | nueva, UUID fijo | `ENFERMERA` | sí |
| Farmacéutico | nueva, UUID fijo | `FARMACEUTICO` | **no** |

El código ADMINISTRACION se mantiene para que `base_completa.sql` siga siendo idempotente: si se lo cambiara, la próxima ejecución crearía un ADMINISTRACION nuevo. Un rol sin código (creado desde la app) cuenta como empleado. El Paciente se identifica **siempre por `codigo='PACIENTE'`**; las comparaciones por nombre se reemplazan.

## Parte 1: regla de roles (código)

**`entity/repository/UsuarioRepository.asignaciones`.** Cambia `r.codigo IN ('ADMINISTRACION','DOCTOR')` por `(r.codigo IS NULL OR r.codigo <> 'PACIENTE')`. Se mantienen `r.activo` y `c.activo`, así que Farmacéutico (inactivo) y Sonsonate (inactiva) quedan fuera.

**`control/service/AutenticacionService`.**
- `ROLES_EMPLEADO` se reemplaza por `esEmpleado(PersonaRol)`: rol activo y código distinto de PACIENTE.
- Es público y estático para reutilizarlo en el filtro y en AtencionService.
- El rol inicial sigue siendo la asignación de empleado más antigua.
- Los mensajes 401 y 403 no cambian.

**`boundary/resources/provider/PermisosApiFilter`.**
- La lista blanca `{ADMINISTRACION, DOCTOR, PACIENTE}` (línea 44) se reemplaza por "empleado, o PACIENTE con su rama actual". El código puede ser null, así que se usa un `String codigo = Objects.toString(..., "")`.
- **`recepcion`, escritura de catálogos y de personas:** siguen siendo exclusivas de `ADMINISTRACION` (Atención al público).
- **`consultas`:** se quita "ADMINISTRACION no lee consultas" (Marta debe abrir SA-6). Las escrituras se abren a cualquier empleado, pero solo por acciones (`sesion`, `cierre`) y por `POST personas-roles/{id}/consultas` de ADMINISTRACION. El servicio decide quién puede hacer cada acción.
- **`CLINICOS`:** se quitan `rol.equals("ADMINISTRACION")` y `!rol.equals("DOCTOR")`. Cualquier empleado de la clínica lee, y las escrituras siguen limitadas a las acciones del flujo. AtencionService exige el rol del paso.
- **Sin cambios:** la verificación de clínica, la de propietario y la rama PACIENTE.

**`control/service/AtencionService`.**
- **`medicoActivo()` → `empleadoActivo()`:** asignación activa, de empleado, con clínica. Se quita el respaldo por nombre. Lo usan `cargar`, `historial`, `pacientesClinica` y `comprobarClinica`.
- **`operadorConsulta()`:** igual (ADMINISTRACION o empleado).
- **Nuevo `exigirResponsable(paso)`:** el `idRol` del rol activo debe ser igual a `paso.idProcedimientoPaso.idRol`. La clínica ya se valida con `comprobarClinica`. Si no coincide: `ServiceException` `consulta.noResponsablePaso` → 403 "Este paso lo atiende el rol {0}." Se aplica en `completar`, `retroceder`, los dos `ordenar`, `examen(...)` (crea y asocia examen) y `guardar` cuando hay un paso pendiente. Un paso histórico sin definición: `consulta.pasoSinDefinicionHistorica` (ya existe).
- **`completar`:** además guarda `paso.idPersonaRol = rol activo`. Así "quién lo realizó" (HU3) es quien lo completó de verdad y no el asignado automático.
- **`retroceder`:** exige el rol del paso actual. El paso reabierto conserva su `idPersonaRol`.
- **`cerrar`:** para cada procedimiento se toma el paso `indicaFin` completado más reciente. Su `idPersonaRol` debe ser el rol activo; si no, `consulta.soloFinalizador` → 403 "Cierra la consulta quien completó el paso de finalización."
- **Seleccionar una consulta (`PUT consultas/{id}/sesion` y `abrir` de JSF).** Se permite solo si:
  - la consulta tiene un paso PENDIENTE de mi rol, o
  - todos sus pasos están completados y yo completé la finalización.

  Si no, `consulta.sinPasoParaRol` → 409.
- **`asignarResponsable`:** lo puede hacer el responsable del paso; la validación de rol del asignado no cambia.
- **`iniciarConsulta` y `pacientesClinica`:** identifican al paciente por `codigo='PACIENTE'`.
- **Locales:** las claves nuevas o renombradas se agregan en los 6 `Messages*.properties`. `consulta.soloMedico` se convierte en `consulta.soloEmpleado` y `consulta.medicoClinica` en `consulta.rolClinica`. Las claves viejas se conservan si JSF aún las usa; lo verifico con grep.
- **Mapeo de errores:** confirmo en `ApiExceptionMapper` cómo se traduce `ServiceException` a 403 o 409. Si no hay forma de dar un 403, uso `AccesoException`.

**`control/service/DashboardService` (pendienteParaMi).** La consulta queda pendiente para mí si:

- tiene un paso PENDIENTE cuyo rol de definición (con respaldo en el rol del responsable, para pasos históricos) es mi rol, **o**
- está abierta, todos sus pasos están completados y yo completé la finalización (me falta cerrarla).

Eso exige agregar `resp.idPersonaRol` (índice 18) a `DashboardRepository.pasos` y pasar el `idPersonaRol` activo a `armar`. También se corrige el Javadoc ("DOCTOR o ADMINISTRACION").

**Otros:** `ConsultaResource:51` se queda igual (Atención al público, al crear una consulta desde recepción, no queda atendiéndola). `PersonaRolRepository:23` y `CatalogoInicialService:21` pasan a `codigo='PACIENTE'`.

**Pruebas JUnit, con TDD: primero en rojo, después la implementación.**
- `AutenticacionServiceTest`:
  - ENFERMERA y un rol sin código entran;
  - PACIENTE da 401;
  - un rol inactivo (FARMACEUTICO) da 401.
- `UsuarioRepositoryTest`: la cláusula nueva.
- `PermisosApiHttpTest` y `PermisosApiFilterCuerpoTest`:
  - ENFERMERA lee `consultas/{id}` y `consultas-procedimientos-pasos`, y no puede escribir catálogos ni recepción;
  - ADMINISTRACION lee consultas.
- `AtencionServiceTest`:
  - Enfermera completa el Triaje, y el Doctor no puede completarlo (403);
  - el Doctor ordena, y la Enfermera no puede ordenar en Consulta médica;
  - retroceso con un rol ajeno da 403;
  - quien completa queda como `idPersonaRol`;
  - cierre por el finalizador da 200, y por otro empleado da 403;
  - sesión sin paso de mi rol da 409.
- `DashboardServiceTest`: `pendienteParaMi` por rol, y por "falta cerrar".
- `AuthResourceTest`, `RestApiTest`, `RecepcionResourceTest` y `RecepcionHttpTest`: se ajustan los que suponen "solo DOCTOR".

**Documentación:**
- `docs/autenticacion.md`: la regla de empleado.
- `docs/api-rest.md`: permisos por rol y errores nuevos de `finalizacion`, `retroceso`, `examenes`, `cierre` y `sesion`.
- `docs/flujo-consulta.md`: responsable por paso y cierre.
- `docs/acceso-base-datos.md`, si menciona los códigos.

## Parte 2: datos demo del guion

### Qué se puede representar en el modelo actual

| Del archivo | ¿Se puede? | Cómo y qué falta |
|---|---|---|
| Tipo de clínica | **Sí** | `clinica.tipo varchar(20)` existe: "General", "Especialidades" y "Pediátrica" caben. |
| Estado de clínicas, roles, tipos y exámenes | Sí | Columnas `activo`. |
| "Es empleado" del rol | Sí, derivado | No hay columna: es "código distinto de PACIENTE" (parte 1). |
| Columna "Formato" de los tipos de documento y de medio de contacto | **No hay columna** | Solo existen `indicaciones` y `expresion_regular`. El formato se escribe en `indicaciones`, por ejemplo "4 dígitos, guion, 4 dígitos; empieza con 2, 6 o 7. Ejemplo: 7123-4567". |
| Orden de los pasos y "paso inicial" | **No hay columna de orden** | Se deduce de las secuencias SIGUIENTE (1→2, 2→3). El inicial es el que no tiene predecesor, igual que hoy. |
| Finalización | Sí | `procedimiento_paso.indica_fin`. |
| Exámenes asignables, incluido Electrocardiograma inactivo | Sí | `procedimiento_paso_examen` (asociación activa) y `examen.activo=false`. |
| Tipo de examen de cada examen | Sí | `examen_tipo_examen`. |
| Fechas de nacimiento, documentos, teléfono y correo | Sí | `persona`, `documento` (único por tipo y valor), `medio_contacto`. |
| Persona sin rol (Pedro) | Sí | Persona sin `persona_rol` ni `usuario`. |
| "Usuario (login)" | Sí, con lo que exige `usuario` | `nombre_usuario` (sin @, por ejemplo `carlos.gomez`), `correo` y `password_hash` argon2 aleatorio de `preparar-bd-desarrollo.sh`. El login sigue siendo por documento. |
| Responsable de cada paso de consulta | Sí | `consulta_procedimiento_paso.id_persona_rol`. |
| Órdenes en SA-1 y SA-6 | Sí | `orden_examen.id_examen`. |
| "Atención al público" como rol de catálogo con 5 roles exactos | Sí, con el renombre | `UPDATE rol SET nombre='Atención al público' WHERE codigo='ADMINISTRACION'`. |
| Ana "34 años" | Sí | 1992-04-10 da 34 años hoy (2026-11-13). La edad se calcula y no se guarda. |

Nada del archivo exige cambiar el esquema, así que no hace falta una migración de esquema.

### Archivos

Los UUID fijos usan el prefijo nuevo `de12xxxx`, uno por tabla, para que no choquen con los viejos `de11xxxx`:

| Prefijo | Tabla |
|---|---|
| `de120000` | clínicas |
| `de120001` | roles nuevos |
| `de120002` | tipos de medio de contacto |
| `de120003` | tipos de documento |
| `de120004` | tipos de examen |
| `de120005` | exámenes |
| `de120006` | clasificaciones |
| `de120007` | procedimientos |
| `de120008` | pasos |
| `de120009` | secuencias |
| `de12000a` | paso-examen |
| `de12000b` | personas |
| `de12000c` | documentos |
| `de12000d` | medios de contacto |
| `de12000e` | `persona_rol` |
| `de12000f` | usuarios |
| `de120010` | consultas |
| `de120011` | `consulta_procedimiento` |
| `de120012` | pasos de consulta |
| `de120013` | órdenes |

Se reutilizan el DUI existente (por nombre, como hoy) y los roles ADMINISTRACION, DOCTOR y PACIENTE (por código).

1. **`docs/migrations/20261113_limpieza_demo_anterior.sql`** (nuevo). Primero comprueba que no haya filas ajenas colgando de las filas de11: consultas o pasos de otras personas que usen el procedimiento demo, o asignaciones ajenas en sus clínicas. Si las hay, aborta con un mensaje. Si no, borra las filas `de11000…` en orden de dependencias. `preparar-bd-desarrollo.sh` **no** lo llama; lo ejecutas tú.
2. **`docs/migrations/20261113_datos_demo_rubrica.sql`** (nuevo; necesita `password_hash`, igual que el viejo `20261111_datos_desarrollo.sql`).
   - Contenido: catálogos (clínicas, renombre de rol, roles nuevos, tipos de medio, tipos de documento, tipos de examen, exámenes, procedimientos, pasos, secuencias y paso-examen), personas, documentos, medios de contacto, asignaciones y usuarios.
   - Usa `ON CONFLICT (id) DO UPDATE` en los catálogos demo, así que repetirlo deja exactamente los valores del archivo.
   - Termina con verificaciones de conteo; si fallan, aborta.
3. **`docs/migrations/20261113_datos_demo_rubrica_consultas.sql`** (nuevo).
   - Contiene las 11 consultas, sus procedimientos, pasos y órdenes, con el mismo diseño que el `…_clinicos.sql` actual: `pg_temp.dia(días, hora)` en America/El_Salvador, tablas temporales con el estado deseado, re-anclaje de fechas y `\if :{?reiniciar}`.
   - El modo reinicio borra resultados, órdenes, pasos y procedimientos agregados desde la app y **restaura las órdenes demo** (Hemograma en SA-1, Glucosa en SA-6). Hoy borra todas las órdenes.
   - Valores con los textos de ejemplo del archivo. Responsables según el archivo; en las consultas cerradas, quien completó cada paso.
4. **Se eliminan** `20261111_datos_desarrollo.sql`, `20261112_datos_desarrollo_clinicos.sql` y `20261113_datos_desarrollo_examenes.sql`, y sus pruebas `tests/datos_desarrollo_clinicos_test.sql` y `tests/datos_desarrollo_examenes_test.sql`. Siguen en el historial de git.
5. **`scripts/preparar-bd-desarrollo.sh`:** carga los dos archivos nuevos en lugar de los tres viejos. **`scripts/reiniciar-demo.sh`:** apunta a `…_consultas.sql` y su texto habla de "las 11 consultas demo (de120010-…)".
6. **`docs/migrations/tests/datos_demo_rubrica_test.sql`** (nuevo, solo lee). Comprueba:
   - 3 clínicas (2 activas) con su tipo;
   - 5 roles (4 activos), con Atención al público con código ADMINISTRACION;
   - 3 y 3 tipos de medio y de documento (2 activos con expresión regular);
   - 3 tipos de examen y 5 exámenes;
   - los asignables de Consulta médica: 4 activos más Electrocardiograma, que se filtra;
   - 9 personas, Pedro sin rol, 5 usuarios;
   - Carlos con 2 asignaciones de Doctor, Jorge solo en San Salvador;
   - las 11 consultas con su día relativo (SA-3 a SA-6 hoy, SA-7 en +3, etc.), sus estados y responsables;
   - las 2 órdenes;
   - que cada paso esté asignado a alguien con el rol del paso en la clínica de la consulta.

   Va acompañado de `datos_demo_rubrica_limpieza_test.sql`, que comprueba que no queden filas de11.
7. **`docs/LEVANTAR.md`** (secciones 5, 7, 9, 10 y la de errores frecuentes) y **`docs/autenticacion.md`**. Cambian:
   - la tabla de documentos y cuentas del §10 del archivo (`01000001-1` …, Pedro rechazado, `12345678-9` neutro);
   - el ejemplo de curl con el DUI de Carlos;
   - "11 consultas";
   - cómo ejecutar la limpieza en una base existente.

**Qué dejo sin verificar:** no ejecuto docker ni psql (CLAUDE.md). Las pruebas SQL las ejecutas tú con los comandos que dejo en LEVANTAR.md. Yo verifico con `mvn test`.

## Parte 3: pendientes de la B6

**Salir de la consulta en curso sin cerrarla:** `DELETE /api/atencion/consulta`.
- Va en `AtencionResource`, que ya es el dueño del contexto (`GET /api/atencion` devuelve `idConsulta`).
- No pasa por `PermisosApiFilter`, igual que el resto de `/atencion`, y no obliga a abrir DELETE en `consultas`.
- Exige sesión vigente (401 `auth.sesion`) y revalida la asignación con `autenticacion.contexto`.
- Hace `sesion.setConsulta(null)` y responde 204. Si no había consulta en curso, también 204 (es idempotente).
- La consulta queda abierta y sin cambios.
- Después se puede cambiar de rol (`AtencionSesion.cambiarRol` ya no bloquea) o seleccionar otra consulta.
- Pruebas en `AtencionResourceTest` (nuevo) o en la clase de pruebas de recurso que corresponda: sin sesión da 401; con consulta, 204 y `GET atencion` devuelve `idConsulta` null; sin consulta, 204; la consulta sigue con `fechaFin` null.
- Se documenta en `api-rest.md` (tabla de `/atencion` y flujo HU4).

**El tipo de examen en `examenes-asignables`:**
- `ExamenAsignable(idExamen, nombre)` pasa a `ExamenAsignable(idExamen, nombre, List<TipoExamenAsignable> tiposExamen)`, donde `TipoExamenAsignable(idTipoExamen, nombre)` lista solo los tipos activos, ordenados por nombre.
- Es una lista porque `examen_tipo_examen` permite varios tipos.
- Una segunda consulta con `IN (:examenes)` trae todos los tipos de una vez, sin N+1.
- **Regla nueva del guion ("exámenes activos con tipos activos"):** un examen es asignable solo si tiene al menos un tipo activo. La misma condición se aplica en `ordenar`.
- Pruebas: el tipo aparece; un examen cuyo único tipo está inactivo no se lista y `ordenar` lo rechaza.
- `api-rest.md` se actualiza con el JSON de ejemplo.

## Orden de los commits

Cada commit compila y pasa `mvn test`. Con los datos viejos o con los nuevos, la app no queda rota.

1. **Copia del plan** a `docs/reportes/b8-plan.md`.
2. **Empleado = cualquier rol activo excepto Paciente.** `UsuarioRepository`, `AutenticacionService` y sus pruebas, más `autenticacion.md`. Con los datos viejos, admin.demo y doctor.demo siguen entrando.
3. **Permisos REST para todos los empleados.** `PermisosApiFilter`, sus pruebas y `api-rest.md`. Las escrituras clínicas todavía las limita AtencionService a DOCTOR, así que no se abre nada antes de tiempo.
4. **Responsable por paso y cierre por el finalizador.** `AtencionService`, mensajes, pruebas y `flujo-consulta.md`. Con la demo vieja (todos los pasos DOCTOR), doctor.demo sigue completando y cerrando.
5. **pendienteParaMi por rol y por cierre pendiente.** `DashboardService`, `DashboardRepository`, pruebas y `api-rest.md`.
6. **`DELETE /api/atencion/consulta`.** Recurso, pruebas y `api-rest.md`.
7. **Tipo de examen en `examenes-asignables`.** Servicio, DTO, pruebas y `api-rest.md`.
8. **Datos demo del guion.** Los 2 SQL nuevos, el script de limpieza, las pruebas SQL, los dos scripts `.sh` y la eliminación de los SQL viejos, todo en un solo commit para que `preparar` y `reiniciar` nunca apunten a archivos que no existen. En el mismo commit van `LEVANTAR.md` y la tabla de demo de `autenticacion.md`. Las pruebas Java que usan UUID o documentos demo viejos como constantes se pasan a los nuevos aquí; si solo usan mocks, no se tocan.
9. **Reporte** `docs/reportes/b8-reporte.md` con los resultados reales.

Los commits son pequeños, en la rama `backend-rubrica`, sin force push y con la línea Co-Authored-By. No se tocan `nbactions.xml` ni los archivos sin seguimiento que ya estaban (`generar_tests.sh`, `inyectar_*.py`).

## Verificación

- `mvn test` después de cada commit; el reporte incluye la salida real.
- Pruebas SQL, que ejecutas tú: `preparar-bd-desarrollo.sh` en una base nueva, luego `psql -f tests/datos_demo_rubrica_test.sql`; una segunda carga, que debe dejar todo idéntico (idempotencia); `reiniciar-demo.sh` después de modificar SA-5, y otra vez el test. En una base vieja: la limpieza y luego su test.
- Prueba manual con Liberty, que dejo descrita en `LEVANTAR.md`:
  1. Login con el DUI de Lucía y completar el Triaje de SA-5.
  2. Con Carlos, verificar que no puede completar el Triaje (403).
  3. Con Carlos, ordenar en SA-3 y ver `tiposExamen`.
  4. Con Marta, completar SA-6 y cerrarla; Carlos no puede cerrarla.
  5. `DELETE /api/atencion/consulta`, y después `PUT atencion/rol-activo`.
  6. Con Ana y con Pedro, el login da 401.

## Adiciones aprobadas

- **LEVANTAR.md:** una sección para quien ya tiene la base con la demo anterior, con los pasos en este orden: respaldo (`pg_dump`), script de limpieza, `preparar-bd-desarrollo.sh` y prueba SQL.
- **Reporte final:** una sección "Contrato para el frontend" con:
  - qué roles pueden iniciar sesión;
  - qué devuelven `/auth/me` y `/atencion/asignaciones` con un rol ENFERMERA;
  - cada código de error nuevo, con su status HTTP y su mensaje;
  - `DELETE /api/atencion/consulta`;
  - la forma nueva de `examenes-asignables`.
- **Ejecución:** `mvn test` antes de cada commit y sin push. Me detengo antes del commit 8 para darte los comandos exactos del contenedor (respaldo, limpieza y datos nuevos).
