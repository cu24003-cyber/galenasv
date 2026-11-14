# Reporte B6: GET /api/atencion/consultas/{id}, acciones con cuerpo vacío y rutas restringidas

Fecha: 2026-11-13. Rama `backend-rubrica`. Sin push.

## Resultado

Se implementó el plan B6 con las decisiones aprobadas:

1. DOCTOR y ADMINISTRACION.
2. 404 `consulta.noExiste`.
3. Sin límite de fecha.

También se hicieron los dos puntos que agregaste:

4. Las acciones aceptan un cuerpo vacío con `Content-Type: application/json`.
5. Las 5 rutas anidadas que dan 403 se documentan como restringidas.

`mvn test`, al final: **751 pruebas, 0 fallos, 0 errores, 0 omitidas**. Al cierre de la B5
eran 731.

| Commit | Contenido |
| --- | --- |
| `af7103a` | `DashboardRepository.consulta(id, clinica)` y `DashboardService.consulta(id)`, con sus pruebas. |
| `a20f6b6` | `GET /api/atencion/consultas/{idConsulta}`. Pruebas en `RestApiTest` y `AutenticacionFilterTest`. |
| `8477070` | `PermisosApiFilter`: en las acciones, un cuerpo vacío o en blanco se reenvía como `{}`. |
| `aea1d2f` | `api-rest.md` y `openapi.json`. |

## B6: una sola consulta

- **`DashboardRepository.consulta(id, clinica)`:** usa las mismas columnas que `consultas()`,
  tomadas de una constante compartida. Filtra con `c.idConsulta=:id AND pr.idClinica.idClinica=:clinica`
  en el mismo WHERE, sin fechas. Una consulta inexistente y una de otra clínica dan la misma lista
  vacía.
- **`DashboardService.consulta(id)`:** hace `exigirSesion()` y `asignacion()` antes de leer datos,
  y después llama a `armar()` sin cambios. Son 4 proyecciones, sin N+1.
- **`AtencionResource`:** `GET consultas/{idConsulta}` responde con `DashboardDto.consulta(...)`,
  que pasó a `public`.

Pruebas nuevas:

- **Repositorio:** el SELECT es el mismo que el del dashboard, el id y la clínica van en el mismo
  WHERE y no hay fechas.
- **Servicio:** el record de la consulta es igual al elemento del dashboard; el 404; las
  validaciones de sesión, rol activo y asignación, sin tocar el repositorio; una consulta de hace
  un año; ADMINISTRACION.
- **REST:** el JSON es igual al elemento del dashboard y los errores llegan con su código. Un id
  que no es UUID da 404 sin llamar al servicio.
- **`AutenticacionFilterTest`:** sin sesión da 401.

### Prueba con curl contra Liberty (solo lecturas)

| Caso | Resultado |
| --- | --- |
| DOCTOR clínica 1 (`de110002-…-2`), consulta `…-2` (hoy) | 200, **idéntica** a su elemento de `GET atencion/dashboard`; `pendienteParaMi` true. |
| DOCTOR clínica 1, consulta `…-1` (2026-10-24, fuera del dashboard de hoy) | 200 |
| DOCTOR, consulta `…-4` (clínica 2) e id inexistente | 404 `consulta.noExiste` en los dos; las respuestas solo difieren en `path` y `timestamp`. |
| DOCTOR, id que no es UUID | 404 |
| ADMINISTRACION clínica 1 (`de110002-…-6`), consulta `…-2` | 200, idéntica a su elemento del dashboard de ADMINISTRACION; `pendienteParaMi` false. |
| ADMINISTRACION, consulta `…-4` (clínica 2) | 404 `consulta.noExiste` |
| Sin sesión | 401 `auth.sesion` |

## Punto 4: acciones con cuerpo vacío

**Causa.** En Liberty, `hasEntity()` es true cuando llega `Content-Type` aunque el cuerpo esté
vacío. Entonces `PermisosApiFilter` intentaba leer el JSON y respondía 400 `json.invalido`.

Hay un segundo problema: cierre y finalización reciben un DTO. Un cuerpo vacío ni siquiera llega
al método del recurso, porque Jersey responde `http.400`. Por eso no bastaba con quitar
`@NotNull`.

**Cambio.** Cuando la ruta es una **acción** que el filtro reconoce, un cuerpo vacío o en blanco
se reenvía como `{}`:

- retroceso y sesión no lo leen;
- finalización completa el paso sin valor;
- cierre conserva la referencia y las observaciones;
- un cuerpo que no es JSON sigue dando 400 `json.invalido`;
- fuera de las acciones no cambia nada: un POST vacío a una colección sigue dando 400.

**Pruebas.**

- **`PermisosApiFilterCuerpoTest`:** prueba unitaria con un `ContainerRequestContext` simulado,
  con `hasEntity()` en true y el cuerpo vacío, porque Jersey no reproduce ese caso de Liberty.
  Cubre:
  - cuerpo vacío o en blanco: se reenvía `{}`;
  - cuerpo mal formado: 400;
  - JSON válido: se reenvía intacto;
  - fuera de las acciones: no se reemplaza.
- **`PermisosApiHttpTest`:** las 4 acciones con cuerpo vacío o en blanco dan 200, y el recurso
  recibe `{}`.
- **`RestApiTest`:** las 4 acciones aceptan `{}`; un cuerpo mal formado da 400 sin llamar al
  servicio.

**Curl contra Liberty,** con doctor.demo y `Content-Type: application/json`. Estas peticiones
**escriben**:

| # | Petición | Resultado |
| --- | --- | --- |
| 1 | `PUT consultas/…-2/sesion`, sin cuerpo | 200 (antes de este cambio: 400 `json.invalido`) |
| 2 | `PUT …/{paso 2}/finalizacion`, sin cuerpo | 200, COMPLETADO sin valor; se creó el paso 3. |
| 3 | `PUT …/{paso 3}/retroceso`, cuerpo `" "` | 200: borró el paso 3 y reabrió el paso 2. |
| 4 | `PUT consultas/…-2/cierre`, sin cuerpo | 409 `consulta.completeAntesCerrar`, del negocio, no `json.invalido`. La consulta no se cerró. |
| 5 | `PUT …/finalizacion` con el cuerpo `x` | 400 `json.invalido` |

## Punto 5: rutas restringidas

En `api-rest.md`, las 5 rutas quedan marcadas como **Restringida** en la tabla de rutas anidadas.
Hay una nota con el motivo y una alternativa para cada una; todas las alternativas respondieron 200
con DOCTOR en la verificación de la B5.

| Ruta | Motivo | Alternativa |
| --- | --- | --- |
| `personas-roles/{id}/pasos-consulta` | El filtro no reconoce el segmento. | `consultas-procedimientos/{id}/pasos` |
| `procedimientos-pasos/{id}/pasos-consulta` | Igual que la anterior. | `consultas-procedimientos/{id}/pasos` |
| `procedimientos/{id}/consultas-procedimientos` | Padre de catálogo; exige `idConsulta`. | `consultas/{id}/procedimientos` |
| `tipos-documento/{id}/documentos` | Padre de catálogo; exige `idPersona`. | `personas/{id}/documentos` |
| `tipos-medio-contacto/{id}/medios-contacto` | Padre de catálogo; exige `idPersona`. | `personas/{id}/medios-contacto` |

## Datos de prueba que quedaron en la demo

En la consulta `…-2`:

- la orden de Hemograma de la B5, en el paso 2;
- el paso 2 se completó y se reabrió. Quedó PENDIENTE, sin valor y con su `fechaInicio` original.
  El paso 3 se creó y se borró, así que no queda.

En mi sesión de curl, que no es la tuya, la consulta `…-2` quedó seleccionada.

**Corre `scripts/reiniciar-demo.sh`.**
