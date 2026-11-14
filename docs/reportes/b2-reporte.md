# Reporte B2: GET /api/atencion/asignaciones y 409 de rolCambio.cerrarAntesCambiar

Fecha: 2026-11-13. Rama: `backend-rubrica`. Plan: `docs/reportes/b2-plan.md`, aprobado con
la opción A para `activa` y un tercer commit para el 409.

Resultado: 3 commits, `mvn test` en verde antes de cada uno, la prueba con curl contra Liberty
pasó, y **el push falló** por autenticación SSH (ver al final).

## Commits

| Commit | Contenido | `mvn test` antes del commit |
| --- | --- | --- |
| `fae7cb9` | `GET /api/atencion/asignaciones`: `AtencionResource`, `AtencionDto.Asignacion`, 5 pruebas en `RestApiTest` y un caso más en `AutenticacionFilterTest` | 640 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `e74b74e` | Documentación: `api-rest.md`, `autenticacion.md`, `openapi.json` (ruta, esquema `AtencionAsignacion` y errores de `rol-activo` con el 400 que había entonces) | 640 / 0 / 0 / 0 |
| `90bc2cf` | `rolCambio.cerrarAntesCambiar` en `CONFLICT` de `ApiExceptionMapper` (409); prueba del mapper y del `PUT rol-activo`; la documentación pasa a 409 | 642 / 0 / 0 / 0 |

Antes había 634 pruebas. En cada commit, `git status` mostró solo los archivos de ese paso.
`nbactions.xml`, `generar_tests.sh`, `inyectar_*.py` y `docs/reportes/` siguen fuera de los
commits.

### TDD

- Commit 1: las 4 pruebas de la ruta fallaron primero con `404`, porque la ruta no existía.
  La de `cambiarRol` con consulta abierta pasó desde el inicio, porque documentaba el 400 de
  entonces. El caso de `AutenticacionFilterTest` también pasó desde el inicio, porque el filtro
  servlet ya exigía la sesión.
- Commit 3: con la prueba cambiada a 409, fallaron 2 pruebas con `expected: <409> but was: <400>`:
  la del recurso y el caso nuevo de `erroresDeServicios`. Pasaron después de agregar la clave a
  `CONFLICT`. También se agregó `consulta.cerrarAntesCrear` a ese parametrizado como referencia.

## Implementación

- `GET /api/atencion/asignaciones` llama a `ContextoRolService.listar()` y convierte cada fila
  con `AtencionDto.Asignacion.de(asignacion, idRolActivo)` sin volver a ordenar.
- `activa` es `true` solo en la asignación que es el rol activo de la sesión
  (`AtencionSesion.getRolActivo()`).
- El recurso lanza `401 auth.sesion` si `!acceso.vigente()`, antes de llamar a `listar()`,
  porque `listar()` devuelve `[]` sin sesión y `PermisosApiFilter` no limita `/atencion`.
- Sin cambios de esquema ni migración. No se tocó `PermisosApiFilter`.

## Prueba contra Liberty

No hubo que reiniciar Liberty. La aplicación está desplegada en modo loose sobre
`target/classes`, y `messages.log` muestra `CWWKZ0003I: The application galenosv updated` a
las 09:31:51, después de la última compilación (`AtencionResource.class` y
`ApiExceptionMapper.class` a las 09:31:45).

```bash
API=http://localhost:9080/galenosv/api
ORIGEN='Origin: http://localhost:9080'
DUI=de110004-0000-4000-8000-000000000001
J=<archivo temporal de cookies>
```

Login con `00000000-2` (doctor.demo):

```bash
curl -s -c $J -b $J -w 'HTTP %{http_code}\n' -X POST "$API/auth/login" -H "$ORIGEN" \
  -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-2\"}"
```

```
HTTP 204
```

Asignaciones con sesión:

```bash
curl -s -c $J -b $J -w '\nHTTP %{http_code}\n' "$API/atencion/asignaciones"
```

```
[{"activa":true,"codigoRol":"DOCTOR","idClinica":"de110000-0000-4000-8000-000000000001","idPersonaRol":"de110002-0000-4000-8000-000000000002","nombreClinica":"Clínica de demostración","nombreRol":"Doctor"},{"activa":false,"codigoRol":"DOCTOR","idClinica":"de110000-0000-4000-8000-000000000002","idPersonaRol":"de110002-0000-4000-8000-000000000005","nombreClinica":"Clínica de demostración Norte","nombreRol":"Doctor"},{"activa":false,"codigoRol":"ADMINISTRACION","idClinica":"de110000-0000-4000-8000-000000000001","idPersonaRol":"de110002-0000-4000-8000-000000000006","nombreClinica":"Clínica de demostración","nombreRol":"Administración"}]
HTTP 200
```

- 3 filas: DOCTOR en la clínica 1, DOCTOR en la clínica 2 y ADMINISTRACION en la clínica 1.
- Una sola fila tiene `activa: true`: DOCTOR en la clínica 1, el rol inicial del login.
- El orden es el de `asignaciones()` (fecha de creación). Las dos asignaciones nuevas de la
  demo están fechadas 1 y 2 minutos después del DOCTOR de la clínica 1.
- Las llaves salen en orden alfabético, porque así las escribe Yasson. El contrato no depende
  de ese orden.

Sin sesión:

```bash
curl -s -w '\nHTTP %{http_code}\n' "$API/atencion/asignaciones"
```

```
{"status":401,"code":"auth.sesion","message":"Vuelva a iniciar sesión.","path":"/galenosv/api/atencion/asignaciones","timestamp":"2026-11-13T15:32:57.252620345Z","details":[]}
HTTP 401
```

## Push: falló

```
$ git push -u origin backend-rubrica
ssh_askpass: exec(/usr/lib/ssh/ssh-askpass): No such file or directory
git@ssh.github.com: Permission denied (publickey).
fatal: Could not read from remote repository.
```

No hay un agente SSH en esta sesión (`ssh-add -l`: "Could not open a connection to your
authentication agent"), y la llave pide una frase que no se puede escribir aquí. Faltan por
subir `fae7cb9`, `e74b74e` y `90bc2cf`. No intenté otra vía, como HTTPS o tokens. Para
subirlos: `git push origin backend-rubrica` desde una terminal con tu agente SSH.

## Pendientes y límites

- El 409 de `rolCambio.cerrarAntesCambiar` no se probó contra Liberty. Hacerlo requiere abrir
  una consulta en la base demo y cambiar de rol, es decir, escribir datos. Está cubierto con
  JUnit (recurso y mapper).
- La vista JSF de Cambiar de rol (`CambiarRolModel`) muestra el mensaje de la excepción y no
  depende del código HTTP, así que el cambio a 409 no la afecta.
- Ni este reporte ni `b2-plan.md` ni `prueba-hu1-liberty.md` están en un commit.
