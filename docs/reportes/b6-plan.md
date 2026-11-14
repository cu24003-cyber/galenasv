# Plan B6: GET /api/atencion/consultas/{id} (HU3 y HU4)

Fecha: 2026-11-13. Rama `backend-rubrica`. Plan sin implementar; espera tu aprobación.

## Objetivo

La HU3 termina con "seleccionar una consulta para poder atenderla", y la HU4 empieza "habiendo
seleccionado una consulta […] desde mi dashboard". Hoy la vista de la consulta tendría que volver
a pedir el dashboard y buscar la consulta en él. El objetivo es leer **una sola consulta** con la
misma forma que un elemento del dashboard.

```
GET /api/atencion/consultas/{idConsulta}         (cookie; sin CSRF por ser GET)
200 {"idConsulta":"…","fechaInicio":"…","fechaFin":null,"pendienteParaMi":true,
     "paciente":{…},"procedimientos":[{…,"pasos":[{…,"orden":1,"nivel":0,…,"tieneValor":true}]}]}
```

El JSON es **idéntico, llave por llave,** al elemento de esa consulta en
`GET /api/atencion/dashboard`. Eso incluye:

- la consulta: `idConsulta`, `fechaInicio`, `fechaFin` y `pendienteParaMi`;
- el paciente, con sus `documentos` y su `edad`;
- los procedimientos, y en cada paso: `orden`, `nivel`, `responsable` y `tieneValor`.

Se conservan los nulos, las fechas van con `-06:00` y no viajan el `valor` de los pasos, las
observaciones, las órdenes ni los resultados. El valor de un paso se sigue pidiendo con
`GET /api/atencion/dashboard/pasos/{id}/valor`.

## Lo que ya existe y se reutiliza

- **`DashboardRepository`:** tiene `consultas(clinica, desde, hasta)` (columnas de la consulta y el
  paciente), `pasos(consultas)`, `documentos(personas)` y `secuencias(procedimientos)`. Todas son
  proyecciones, sin entidades ni el valor de los pasos.
- **`DashboardService.armar(filas, rolActivo, hoy)`:** arma `ConsultaDashboard` a partir de las
  filas de consultas. Calcula `nivel` (Kahn), `orden`, `pendienteParaMi`, la `edad` y los días en
  El Salvador. **Se reutiliza tal cual.**
- **`DashboardService.exigirSesion()` y `asignacion()`:** son las validaciones de sesión y de
  clínica que pediste.
  - 401 `auth.sesion` si no hay sesión vigente o el usuario está inactivo;
  - 403 `auth.rolActivo` si no hay rol activo;
  - 403 `auth.asignacion` si el rol ya no es una asignación de empleado activa de la cuenta.
  - La clínica se lee de la base, nunca del cliente.
- **`DashboardDto.consulta(ConsultaDashboard)`:** ya existe; hoy es package-private.
- **`PermisosApiFilter`** no revisa `/atencion`; `AutenticacionFilter` exige la sesión, como en el
  dashboard.

## Diseño

### `DashboardRepository.consulta(UUID id, UUID clinica)`

Devuelve las **mismas columnas** que `consultas()`: `[idConsulta, fechaInicio, fechaFin,
idPersona, nombres, apellidos, fechaNacimiento]`. La condición es
`c.idConsulta = :id AND pr.idClinica.idClinica = :clinica`, **en el mismo WHERE**:

- un id inexistente y uno de otra clínica dan la misma lista vacía, como en `valorPaso`;
- no se filtra por fecha, porque la HU3 pide consultas pasadas y futuras.

Si `id` o `clinica` son null, devuelve `List.of()` sin consultar. El `SELECT … FROM … JOIN` se
saca a una constante privada que comparten `consultas()` y `consulta()`, para que las columnas no
puedan divergir.

### `DashboardService.consulta(UUID id)`

1. `exigirSesion()` y luego `asignacion()`, antes de cualquier consulta de datos.
2. `repositorio.consulta(id, clinica de la asignación)`. Si viene vacía:
   **404 `consulta.noExiste`**, "La consulta no existe.".
3. `armar(filas, rol de la asignación, hoy).get(0)`.

Son **4 consultas** de datos (consulta, pasos, documentos y secuencias), más las 2 de la
validación de la asignación. Es lo mismo que el dashboard, sin N+1. `pendienteParaMi` se calcula
igual, contra el rol activo.

### `AtencionResource`

```java
@GET @Path("consultas/{idConsulta}")
public Response consulta(@PathParam("idConsulta") UUID id) {
    return Response.ok(DashboardDto.consulta(dashboardService.consulta(id)).toString()).build();
}
```

`DashboardDto.consulta` pasa a `public`. Un id que no es UUID da 404 de la ruta, sin llegar al
servicio, igual que `…/valor`. Como solo lee, el GET **no selecciona** la consulta en la sesión:
para atenderla se sigue usando `PUT /api/consultas/{id}/sesion`.

## Decisiones que necesito de ti

1. **Roles:**
   - **(A, recomendada)** DOCTOR y ADMINISTRACION, como el dashboard del que sale la consulta.
   - (B) Solo DOCTOR, como las operaciones clínicas de la HU4. ADMINISTRACION recibiría 403.
2. **Código del 404:**
   - **(A, recomendada)** `consulta.noExiste`, la misma respuesta para una consulta inexistente
     y para una de otra clínica, así no se revela que existe.
   - (B) Reutilizar `recurso.noExiste`.
3. **Fecha de la consulta:**
   - **(A, recomendada)** Sin restricción de fecha.
   - (B) Limitarla a la ventana de 31 días del dashboard. No lo recomiendo: la HU3 pide consultas
     pasadas y futuras, y un enlace guardado dejaría de funcionar.

## Pruebas (TDD: primero en rojo)

**`DashboardRepositoryTest`:**

1. `consulta` filtra por id y clínica en el mismo WHERE, con parámetros, sin rango de fechas y
   con las mismas columnas que `consultas`. Se compara el texto del SELECT.
2. Con `id` o `clinica` null no consulta.

**`DashboardServiceTest`:**

3. Con los mismos datos simulados, `consulta(id)` es **igual** (`equals` de los records) al
   elemento de esa consulta en `consultas(hoy, hoy)`.
4. Una consulta inexistente o de otra clínica (lista vacía) da 404 `consulta.noExiste`.
5. Sin sesión da 401, sin rol activo da 403 `auth.rolActivo` y con la asignación revocada da 403;
   en los tres casos sin tocar el repositorio.
6. No hay validación de fecha: una consulta de hace un año se devuelve.
7. `pendienteParaMi` y `nivel` de un procedimiento con ramificación, igual que en el dashboard.

**`RestApiTest`:**

8. `GET atencion/consultas/{id}`: 200 con las llaves exactas del elemento del dashboard, nulos
   incluidos (`fechaFin`, `responsable`, `edad`).
9. El 404 `consulta.noExiste` y el 401 del servicio llegan con su código.
10. Un id que no es UUID da 404 y no llama al servicio.

**`AutenticacionFilterTest`:**

11. `/api/atencion/consultas/{id}` sin sesión da 401, si los parametrizados no lo cubren ya.

## Documentación

- **`docs/api-rest.md`**, en la sección "Dashboard de consultas (HU3)":
  - una fila de la operación;
  - "misma forma que un elemento del dashboard";
  - el 404 `consulta.noExiste`;
  - la nota de que no selecciona la consulta en la sesión.
- **`src/main/webapp/openapi.json`:** la ruta, con su esquema de respuesta.

## Prueba con curl contra Liberty (solo lecturas)

| Caso | Esperado |
| --- | --- |
| doctor.demo, consulta `de110009-…-2` (hoy) | 200, idéntica a su elemento de `GET atencion/dashboard`. Se compara con `jq -S`. |
| doctor.demo, una consulta pasada de la clínica 1 fuera del dashboard de hoy | 200 |
| Consulta de la clínica 2 (`…-4`) e id inexistente | 404 `consulta.noExiste` y 404 `consulta.noExiste`; las respuestas solo difieren en `path` y `timestamp`. |
| Id que no es UUID | 404 |
| ADMINISTRACION clínica 1 (`de110002-…-6`) | 200, según la decisión 1 |
| Sin sesión | 401 `auth.sesion` |

## Archivos y commits

| Archivo | Cambio |
| --- | --- |
| `entity/repository/DashboardRepository.java` | `consulta(id, clinica)` y la constante compartida del SELECT. |
| `control/service/DashboardService.java` | `consulta(id)`. |
| `boundary/resources/dto/DashboardDto.java` | `consulta(...)` pasa a `public`. |
| `boundary/resources/AtencionResource.java` | `GET consultas/{idConsulta}`. |
| Pruebas | `DashboardRepositoryTest`, `DashboardServiceTest`, `RestApiTest` y, si hace falta, `AutenticacionFilterTest`. |
| `docs/api-rest.md`, `src/main/webapp/openapi.json` | Documentación. |

Sin cambios de esquema ni migraciones.

Commits, con `mvn test` antes de cada uno:

1. Repositorio y servicio, con sus pruebas.
2. Recurso REST, con sus pruebas.
3. Documentación.

Sin push.
