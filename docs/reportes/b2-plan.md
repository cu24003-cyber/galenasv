# Plan B2: GET /api/atencion/asignaciones

Fecha: 2026-11-13. Rama: `backend-rubrica`. Estado: **esperando aprobación**. No se ha
cambiado código.

## Lo que hay hoy

- `ContextoRolService.listar()` devuelve `autenticacion.asignaciones(idUsuario)` si
  `acceso.vigente()`. **Si no hay sesión vigente devuelve `List.of()` y no lanza nada.**
  Con un usuario inactivo, `AutenticacionService.usuarioActivo` lanza `401 auth.sesion`.
- `UsuarioRepository.asignaciones(idPersona)` ya filtra rol activo, clínica activa y códigos
  `ADMINISTRACION`/`DOCTOR` (HU2). Ordena por `fechaCreacion, idPersonaRol`. Trae `idRol` e
  `idClinica` con `JOIN FETCH`, así que leer nombres no hace consultas extra.
- `PermisosApiFilter` deja pasar `auth`, `atencion` y `publico` sin revisar el rol. Solo
  `AutenticacionFilter`, el filtro servlet, exige la sesión para `/api/atencion/*`
  (`401 auth.sesion`).
- `PersonaRol` no tiene columna `activo`. Todo lo que devuelve `asignaciones()` ya es una
  asignación activa.

## rolCambio.cerrarAntesCambiar: con qué código llega

`AtencionSesion.cambiarRol` lanza `ServiceException.localizada("rolCambio.cerrarAntesCambiar", …)`
sin causa. En `ApiExceptionMapper` esa clave no está en `NOT_FOUND` ni en `CONFLICT`, y una
`ServiceException` sin causa da **400**. Hoy, entonces, el `PUT /api/atencion/rol-activo` con
una consulta abierta y otra asignación responde:

```
400 {"status":400,"code":"rolCambio.cerrarAntesCambiar","message":"Cierre la consulta antes de cambiar de médico o rol.", …}
```

Es la respuesta que voy a documentar. Antes de que llegue a lanzarse, `AtencionResource.cambiarRol` ya
llamó a `autenticacion.contexto`, que rechaza una asignación ajena con `403 auth.asignacion`.
No llega a `request.changeSessionId()`, así que el id de sesión no cambia.

## Diseño

```
GET /api/atencion/asignaciones        (cookie de sesión; sin CSRF por ser GET)
200 [{"idPersonaRol":"…","codigoRol":"DOCTOR","nombreRol":"Doctor",
      "idClinica":"…","nombreClinica":"Clínica de demostración","activa":true}, …]
401 auth.sesion   sin sesión vigente o usuario inactivo
```

En `AtencionResource`:

```java
@Inject private ContextoRolService roles;

@GET @Path("asignaciones")
public List<AtencionDto.Asignacion> asignaciones() {
    if (!acceso.vigente()) throw new AccesoException(401, "auth.sesion", "Vuelva a iniciar sesión.");
    var activo = sesion.getRolActivo();
    UUID idActivo = activo == null ? null : activo.getIdPersonaRol();
    return roles.listar().stream().map(a -> AtencionDto.Asignacion.de(a, idActivo)).toList();
}
```

- **Sesión obligatoria también en el recurso.** Sin esa comprobación, si un día cambia el
  filtro servlet, `listar()` respondería `200 []` sin sesión. Con ella falla con 401.
- **Orden:** el de `listar()`, es decir el de `asignaciones()`, sin volver a ordenar.
- **DTO:** `AtencionDto.Asignacion`, un `record` con los 6 campos y un `de(PersonaRol, UUID)`.
  No expone persona, fechas ni colecciones.

## Decisión que necesito de ti: qué significa `activa`

Todas las filas ya son asignaciones activas, así que `activa: true` en todas no aportaría
nada. Propongo:

- **(A, recomendada)** `activa` = es el rol activo de esta sesión
  (`idPersonaRol == AtencionSesion.getRolActivo().getIdPersonaRol()`). Una sola fila en `true`
  tras el login o un `PUT rol-activo`. Le sirve a ChivoMed para marcar el rol actual.
- (B) `activa` = siempre `true`, solo para cumplir la forma pedida.

El plan sigue con (A) salvo que digas otra cosa.

## Archivos (7, por eso el plan)

| Archivo | Cambio |
| --- | --- |
| `boundary/resources/AtencionResource.java` | Método `asignaciones()` e inyección de `ContextoRolService`. |
| `boundary/resources/dto/AtencionDto.java` | `record Asignacion` con su `de(...)`. |
| `test/.../boundary/resources/RestApiTest.java` | Pruebas del recurso (abajo). Se agrega el binding del mock de `ContextoRolService`. |
| `test/.../boundary/web/AutenticacionFilterTest.java` | Agregar `/api/atencion/asignaciones` a `recursosClinicosNoAdmitenAnonimos` (GET anónimo da 401). |
| `docs/api-rest.md` | Fila de la nueva ruta, ejemplo de respuesta y códigos de `PUT rol-activo`, incluido el 400 de `rolCambio.cerrarAntesCambiar`. |
| `docs/autenticacion.md` | Fila de la tabla de rutas de sesión (línea 43). |
| `src/main/webapp/openapi.json` | Ruta `/atencion/asignaciones` y respuesta 400 en `/atencion/rol-activo`. |

No cambia el esquema ni hay migración. No se toca `PermisosApiFilter`.

## Pruebas (TDD: primero en rojo y después la implementación)

En `RestApiTest`:

1. `asignacionesDevuelveLosSeisCamposEnElOrdenDelServicio`: `listar()` devuelve 2 asignaciones
   en clínicas distintas. Se comprueban el orden, que cada objeto tenga **exactamente** las
   llaves `idPersonaRol, codigoRol, nombreRol, idClinica, nombreClinica, activa`, y que solo
   la del rol de sesión tenga `activa: true`.
2. `asignacionesSinRolActivoMarcaTodasFalse`: `getRolActivo()` es null.
3. `asignacionesSinSesionDa401`: `acceso.vigente()` es false. Se espera `401 auth.sesion` y
   `verify(roles, never()).listar()`.
4. `asignacionesVaciasDevuelveListaVacia`: `listar()` devuelve `[]`, y la respuesta es `200 []`.
5. `cambiarRolConConsultaAbiertaDa400CerrarAntesCambiar`: `sesion.cambiarRol` lanza la
   `ServiceException` real. Se esperan `400`, `code = rolCambio.cerrarAntesCambiar` y que
   `request.changeSessionId()` no se llame, si el arnés lo permite verificar. Si no, solo el
   status y el código.

En `AutenticacionFilterTest`, un caso más en el `@ValueSource` existente.

## Commits

1. Las pruebas y la implementación de la ruta (`AtencionResource`, `AtencionDto`, `RestApiTest`,
   `AutenticacionFilterTest`).
2. La documentación (`api-rest.md`, `autenticacion.md`, `openapi.json`).

Antes de cada commit: `mvn test` con el resultado real y `git status` solo con los archivos
de ese paso. Sin push. Opcional, si Liberty sigue arriba y lo autorizas: un curl con login de
`doctor.demo` para ver las 3 asignaciones reales (DOCTOR C1, ADMINISTRACION C1 y DOCTOR C2).

## Fuera de alcance

- Cambiar el 400 de `rolCambio.cerrarAntesCambiar` por 409. Sería coherente con
  `consulta.cerrarAntesCrear`, que da 409, pero no lo pediste. Si lo quieres, es agregar la
  clave a `CONFLICT` y una prueba.
- Limitar `/atencion` en `PermisosApiFilter`.
