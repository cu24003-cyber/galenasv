# Reporte B3: dashboard de la HU3

Fecha: 2026-11-13. Rama: `backend-rubrica`. Plan: `docs/reportes/b3-plan.md`, aprobado con
cambios:

1. El dashboard acepta DOCTOR y ADMINISTRACION, sin tocar `AtencionService.historial`.
2. Orden ascendente por fecha de inicio.
3. Máximo 31 días, con 400 y un código claro.
4. Se agrega `pendienteParaMi` por consulta.

Resultado: 3 commits, `mvn test` en verde antes de cada uno, y las 5 pruebas con curl contra
Liberty pasaron. **No se hizo push.**

## Commits

| Commit | Contenido | `mvn test` antes del commit |
| --- | --- | --- |
| `1de2a74` | `DashboardRepository` (5 proyecciones) y `DashboardRepositoryTest` (8) | 650 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `1214d3b` | `DashboardService`, `DashboardDto`, 2 rutas en `AtencionResource`; `DashboardServiceTest` (24), 6 pruebas en `RestApiTest`, 1 caso en `AutenticacionFilterTest` | 681 / 0 / 0 / 0 |
| `bc25c2f` | `api-rest.md`, `autenticacion.md`, `openapi.json` | 681 / 0 / 0 / 0 |

Antes había 642 pruebas. En cada commit, `git status` mostró solo los archivos de ese paso.
`nbactions.xml`, `generar_tests.sh`, `inyectar_*.py` y `docs/reportes/` siguen fuera.

**Commit 2 rehecho con `--amend`.** La prueba con curl mostró que la fecha de nacimiento se leía
con la zona de la JVM (detalle abajo). Corregí eso con una prueba nueva, que verifiqué en rojo
contra el código anterior (`expected: <1999-12-31> but was: <2000-01-01>`), y la incluí en
`1214d3b` con `--amend`. Ese commit no se había subido, así que no hace falta force push.

### TDD

- `DashboardRepositoryTest`, `DashboardServiceTest` y las pruebas nuevas de `RestApiTest`
  fallaron primero por compilación, porque las clases no existían.
- La prueba de la zona horaria de la fecha de nacimiento falló contra el código anterior antes de
  la corrección.
- El caso de `AutenticacionFilterTest` (`/api/atencion/dashboard` anónimo da 401) pasó desde el
  inicio, porque el filtro servlet ya exige la sesión en todo `/api` salvo `/publico`.

## Diferencias con el plan

| Plan | Cómo quedó | Por qué |
| --- | --- | --- |
| Reutilizar `historial` | Consulta propia `DashboardRepository.consultas(clinica, desde, hasta)` | Decisión 1: `historial` solo acepta DOCTOR y lo usa JSF. |
| 403 `auth.permiso` para ADMINISTRACION | ADMINISTRACION permitido | Decisión 1. |
| 400 `dashboard.rango` | `dashboard.rangoInvertido` y `dashboard.rangoExcedido` | Un código claro para cada caso (decisión 3). |
| `consulta.pasoNoExiste` en `NOT_FOUND` de `ApiExceptionMapper` | `AccesoException(404, …)`, sin tocar el mapper | Igual que `cita.noExiste` en HU1. Un archivo menos. |
| `record`s en `boundary/.../dto` | `record`s en `DashboardService`; `DashboardDto` solo arma el JSON | El servicio (control) no depende de boundary, como `CitaPublicaService`. |
| Serializar con JSON-B | JSON-P en `DashboardDto` | JSON-B omite las llaves con null; así `fechaFin`, `responsable`, `edad` y `valor` salen como `null`, igual que `GET /api/atencion`. |
| — | `pendienteParaMi` | Decisión 4. |

## Diseño

- **Validación**, en `DashboardService`, antes de cualquier consulta de datos:
  1. sin sesión vigente: 401 `auth.sesion`;
  2. sin rol activo: 403 `auth.rolActivo`;
  3. fechas: 400 `dashboard.fecha`, `dashboard.rangoInvertido` o `dashboard.rangoExcedido`;
  4. `autenticacion.contexto(idUsuario, rolActivo)`: relee al usuario y sus asignaciones de
     empleado activas (401 `auth.sesion` o 403 `auth.asignacion`). La clínica y el rol salen de
     esa asignación, nunca del cliente.
- **Las 4 consultas sin N+1:**
  - consultas por clínica y rango;
  - procedimientos con sus pasos (`LEFT JOIN` a la colección, la definición, el rol y el
    responsable, y `CASE WHEN p.valor IS NOT NULL` en lugar del valor);
  - documentos de los pacientes;
  - secuencias de los procedimientos.

  Cada una se ejecuta una vez con la lista completa de ids (`IN :lista`). Con 0 consultas no se
  ejecutan las otras 3.
- **`pendienteParaMi`:** existe un paso PENDIENTE cuyo rol es el `idRol` del rol activo. Se toma
  el rol de la definición del paso y, si el paso no tiene definición, el del responsable.
- **Orden de los pasos:** `nivel` es el camino más largo desde un paso inicial del grafo de
  secuencias (Kahn). Los pasos se ordenan por nivel, luego `fechaInicio` y luego id. Un paso sin
  definición va al final.
- **Valor:** una consulta con `id` **y** clínica en el mismo `WHERE`. Un paso inexistente y uno de
  otra clínica dan el mismo 404.

## Prueba contra Liberty

No hubo que reiniciar Liberty. Recarga la aplicación (modo loose) después de cada compilación;
la última, a las 10:00:14, ya incluía la corrección de la fecha.

```bash
API=http://localhost:9080/galenosv/api
ORIGEN='Origin: http://localhost:9080'
DUI=de110004-0000-4000-8000-000000000001
# login con 00000000-2 (doctor.demo): HTTP 204; rol inicial DOCTOR en la clínica 1
curl -s -c $J -b $J -X POST "$API/auth/login" -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-2\"}"
# cambio de rol: PUT rol-activo con el token de GET /api/auth/me
curl -s -b $J -c $J -X PUT "$API/atencion/rol-activo" -H "$ORIGEN" -H "X-CSRF-Token: $TOKEN" \
  -H 'Content-Type: application/json' -d '{"idPersonaRol":"…"}'
```

### 1. Hoy con DOCTOR en la clínica 1

```bash
curl -s -b $J -c $J -w '\nHTTP %{http_code}\n' "$API/atencion/dashboard"
```

```
[{"idConsulta":"de110009-0000-4000-8000-000000000002","fechaInicio":"2026-11-13T08:00:00-06:00","fechaFin":null,"pendienteParaMi":true,"paciente":{"nombres":"Paciente","apellidos":"Demostración","fechaNacimiento":"1999-12-31","edad":26,"documentos":[{"tipoDocumento":"DUI","numero":"00000000-3"}]},"procedimientos":[{"idConsultaProcedimiento":"de11000a-0000-4000-8000-000000000002","nombre":"Consulta general de demostración","fechaInicio":"2026-11-13T08:00:00-06:00","fechaFin":null,"pasos":[{"idConsultaProcedimientoPaso":"de11000b-0000-4000-8000-000000000021","nombre":"Toma de signos vitales","orden":1,"nivel":0,"estado":"COMPLETADO","responsable":{"nombre":"Doctor Demostración","rol":"Doctor"},"fechaInicio":"2026-11-13T08:05:00-06:00","fechaFin":"2026-11-13T08:15:00-06:00","tieneValor":true},{"idConsultaProcedimientoPaso":"de11000b-0000-4000-8000-000000000022","nombre":"Evaluación médica","orden":2,"nivel":1,"estado":"PENDIENTE","responsable":{"nombre":"Doctor Demostración","rol":"Doctor"},"fechaInicio":"2026-11-13T08:15:00-06:00","fechaFin":null,"tieneValor":false}]}]}]
HTTP 200
```

Incluye la consulta a medio avanzar (`…-2`): paso 1 COMPLETADO con `tieneValor: true` y paso 2
PENDIENTE. `pendienteParaMi` es `true`. No aparece ninguna llave `valor`.

El valor del paso 1, pedido aparte:

```bash
curl -s -b $J -w '\nHTTP %{http_code}\n' "$API/atencion/dashboard/pasos/de11000b-0000-4000-8000-000000000021/valor"
```

```
{"idConsultaProcedimientoPaso":"de11000b-0000-4000-8000-000000000021","nombre":"Toma de signos vitales","estado":"COMPLETADO","fechaFin":"2026-11-13T08:15:00-06:00","valor":"Demostración: PA 120/80, FC 72, T 36.6 °C"}
HTTP 200
```

### 2. El mismo día con ADMINISTRACION (clínica 1)

`PUT rol-activo` a `de110002-…-0006` dio `200 {"idPersonaRol":"de110002-0000-4000-8000-000000000006","idConsulta":null}`.

```
[{"idConsulta":"de110009-0000-4000-8000-000000000002","fechaInicio":"2026-11-13T08:00:00-06:00","fechaFin":null,"pendienteParaMi":false, … (mismos paciente, procedimiento y pasos) …}]
HTTP 200
```

Es la misma consulta, con `pendienteParaMi: false`, porque el paso pendiente es del rol Doctor.

### 3. Rango de 40 días

```bash
curl -s -b $J -c $J -w '\nHTTP %{http_code}\n' "$API/atencion/dashboard?desde=2026-10-24&hasta=2026-12-02"
```

```
{"code":"dashboard.rangoExcedido","details":[],"message":"El rango de fechas admite como máximo 31 días.","path":"/galenosv/api/atencion/dashboard","status":400,"timestamp":"2026-11-13T16:01:23.739378842Z"}
HTTP 400
```

Control con 31 días (`2026-10-24` a `2026-11-23`), con DOCTOR en la clínica 1: 200 con 3
consultas en orden ascendente.

```
…-1 2026-10-24T09:00-06:00 cerrada, pendienteParaMi=False: pasos 1/0, 2/1, 3/2 COMPLETADOS con valor
…-2 2026-11-13T08:00-06:00 abierta, pendienteParaMi=True:  1/0 COMPLETADO con valor, 2/1 PENDIENTE
…-3 2026-11-20T10:00-06:00 abierta, pendienteParaMi=True:  1/0 PENDIENTE
```

(`orden/nivel`: la consulta pasada muestra los 3 niveles de la secuencia.)

### 4. Sin sesión

```bash
curl -s -w '\nHTTP %{http_code}\n' "$API/atencion/dashboard"
curl -s -w '\nHTTP %{http_code}\n' "$API/atencion/dashboard/pasos/de11000b-0000-4000-8000-000000000051/valor"
```

```
{"status":401,"code":"auth.sesion","message":"Vuelva a iniciar sesión.","path":"/galenosv/api/atencion/dashboard","timestamp":"2026-11-13T16:01:24.940004017Z","details":[]}
HTTP 401
{"status":401,"code":"auth.sesion","message":"Vuelva a iniciar sesión.","path":"/galenosv/api/atencion/dashboard/pasos/de11000b-0000-4000-8000-000000000051/valor","timestamp":"2026-11-13T16:01:24.976096428Z","details":[]}
HTTP 401
```

Este 401 lo da `AutenticacionFilter` antes de llegar a JAX-RS. La comprobación propia de
`DashboardService` está cubierta con JUnit; contra el servidor no se puede alcanzar sin quitar
el filtro.

### 5. Valor de un paso de la otra clínica

Para obtener el id, cambié a DOCTOR de la clínica 2 (`…-0005`). Su dashboard de hoy trae la
consulta `de110009-…-5` con el paso `de11000b-0000-4000-8000-000000000051`. Luego volví a DOCTOR
de la clínica 1 (`…-0002`).

```bash
curl -s -b $J -c $J -w '\nHTTP %{http_code}\n' "$API/atencion/dashboard/pasos/de11000b-0000-4000-8000-000000000051/valor"
```

```
{"code":"consulta.pasoNoExiste","details":[],"message":"El paso no existe.","path":"/galenosv/api/atencion/dashboard/pasos/de11000b-0000-4000-8000-000000000051/valor","status":404,"timestamp":"2026-11-13T16:01:24.447233528Z"}
HTTP 404
```

Control: el mismo id con la clínica 2 activa da 200
`{"idConsultaProcedimientoPaso":"de11000b-0000-4000-8000-000000000051","nombre":"Toma de signos vitales","estado":"PENDIENTE","fechaFin":null,"valor":null}`.
Al final la sesión de prueba quedó con DOCTOR en la clínica 1. Estas pruebas no escribieron
datos clínicos; solo cambiaron el rol activo de una sesión de curl.

## Hallazgo: fecha de nacimiento de la demo

`persona.fecha_nacimiento` es `timestamp with time zone`, y los datos demo
(`20261111_datos_desarrollo.sql`) la guardan como `2000-01-01T00:00:00Z`. La aplicación captura
y muestra esa fecha en hora de El Salvador (`p:datePicker` y `f:convertDateTime` con
`timeZone="America/El_Salvador"` en `persona-list.xhtml`). Por eso la lista JSF de personas ya
muestra `31/12/1999` para los usuarios demo, y el dashboard da lo mismo: `"1999-12-31"`,
`edad: 26`.

- El dashboard es coherente con la aplicación. La primera versión usaba la zona de la JVM, que
  aquí coincide (CST), pero en otro servidor podía variar. La corregí para usar siempre
  `America/El_Salvador`.
- **El dato demo no se corrigió** (sería otra migración). Si se quiere que la demo muestre el
  1 de enero de 2000, basta guardarla como `2000-01-01T00:00:00-06:00`. Esto afecta también a la
  vista JSF.

## Pendientes y límites

- ADMINISTRACION también puede leer el `valor` de los pasos de su clínica (decisión 1: el
  dashboard y su detalle aceptan los dos roles). Si el valor debe ser solo para DOCTOR, es un
  cambio de una línea en `DashboardService.valor`.
- `historial` y la vista JSF de Historial no cambiaron.
- Ni este reporte ni los de B2 y B3 están en un commit.
- Push: no se hizo, como pediste. `origin/backend-rubrica` ya tiene los commits de B2, así que
  faltan solo `1de2a74`, `1214d3b` y `bc25c2f`.
