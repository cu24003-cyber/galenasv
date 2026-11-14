# Reporte B8: roles por paso, datos demo del guion y pendientes de la B6

Plan aprobado: `docs/reportes/b8-plan.md`. Rama `backend-rubrica`, sin push.

## Commits

| Commit | Contenido |
| --- | --- |
| `cc35227` | Copia del plan. |
| `4d5910a` | Empleado = cualquier rol activo excepto Paciente (login, `/auth/me`, asignaciones). |
| `3af1644` | Permisos REST (`PermisosApiFilter`) para cualquier empleado de la clínica. |
| `d8771ef` | Cada paso lo atiende su rol responsable; cierra quien completó la finalización; `exigirAtendible` en `PUT consultas/{id}/sesion`. |
| `ec05c4a` | `pendienteParaMi` también cuando al empleado le falta cerrar la consulta. |
| `0e5f644` | `DELETE /api/atencion/consulta`: salir de la consulta sin cerrarla. |
| `c154c94` | `tiposExamen` en `examenes-asignables`; un examen sin tipo activo deja de ser asignable. |
| `0856fa3` | `rolPaso {idRol, nombre}` en los pasos del dashboard y de `GET /atencion/consultas/{id}`; `idRol` en `/auth/me`. (Commit adicional pedido antes del 8.) |
| `e3d0902` | Datos demo del guion, script de limpieza de la demo anterior, pruebas SQL, scripts y documentación. |
| este | Este reporte y la corrección de `docs/flujo-consulta.md` ("rol médico activo" → "rol de empleado activo"). |

## Verificación

- `mvn test` antes de cada commit. Última ejecución, sobre el estado final: **793 pruebas, 0 fallos,
  0 errores, 0 omitidas**.
- SQL del commit 8, en un PostgreSQL 18.6 desechable (clúster temporal en el scratchpad, borrado al
  terminar; no se usó Docker ni la base del equipo):
  - base nueva: carga, `tests/datos_demo_rubrica_test.sql`, segunda carga con los datos idénticos
    (hash de pasos y roles igual), cambios simulados desde la app (paso completado, paso nuevo, orden
    agregada, orden demo borrada, consulta cerrada y movida de día), la prueba los detecta, `-v
    reiniciar=1` y la prueba vuelve a pasar;
  - base con la demo anterior más una consulta, paso, orden y resultado creados "desde la app" sobre
    paciente.demo: limpieza dos veces (idempotente), `tests/datos_demo_rubrica_limpieza_test.sql`, y
    el mismo recorrido de la base nueva;
  - limpieza con una asignación de otra persona en "Clínica de demostración": se detiene con "No se
    borró nada… (1 asignación(es) de otras personas en la clínica demo)" y no borra nada.
- **Pendiente:** `src/main/webapp/openapi.json` sigue declarando `codigoRol` (asignaciones) con
  `enum [ADMINISTRACION, DOCTOR]` y `rolActivo.codigo` (`/auth/me`) con `enum [ADMINISTRACION, DOCTOR,
  PACIENTE]`, sin `null`, sin `idRol` ni `rolPaso`. No se tocó en la B8; si el frontend genera tipos
  desde ese archivo, hay que actualizarlo antes. El contrato válido es el de esta sección y `docs/api-rest.md`.
- **Sin verificar todavía:** la ejecución en el contenedor del equipo (comandos abajo) y la prueba
  manual con Liberty y ChivoMed descrita en el plan.

## Desviaciones aceptadas

1. Cualquier empleado puede crear consultas (`POST /api/personas-roles/{id}/consultas`), no solo
   Atención al público.
2. Guardar las notas de la consulta (`guardar`) no exige el rol del paso pendiente.
3. La regla para seleccionar una consulta (`exigirAtendible`, 409 `consulta.sinPasoParaRol`) se aplica
   en REST (`PUT /api/consultas/{id}/sesion`); la vista JSF no la aplica.

## Contrato para el frontend

Todo lo que cambió para el cliente en la B8. Los errores siguen el formato de siempre:
`{"status", "code", "message", "path", "timestamp", "details"}`; el frontend debe decidir por `code`
y mostrar `message`.

### Quién puede iniciar sesión (HU2)

- **Empleado** = cualquier asignación cuyo rol esté activo y no sea `PACIENTE`, en una clínica activa.
  Incluye Atención al público (`ADMINISTRACION`), Doctor, **Enfermera** y cualquier rol creado desde la
  app, aunque no tenga código.
- Rechazados con el mismo 401 `auth.credenciales` ("Documento no registrado o sin acceso
  autorizado."): solo Paciente, sin asignación, rol inactivo (Farmacéutico), clínica inactiva
  (Sonsonate), documento inexistente.
- El rol inicial sigue siendo la asignación de empleado más antigua.
- El nombre visible de `ADMINISTRACION` es ahora **"Atención al público"**; el código no cambia.

Con la demo nueva: entran Carlos (`01000001-1`), Lucía (`01000002-2`), Marta (`01000003-3`), Rosa
(`01000006-6`) y Jorge (`01000007-7`); Ana (`01000004-4`), Julia, Juan y Pedro (`01000005-5`) reciben
401.

### `GET /api/auth/me`

`rolActivo` tiene ahora exactamente estas llaves:
`idPersonaRol, idRol, codigo, nombreRol, idClinica, nombreClinica`.

- `idRol` (nuevo): UUID del rol; se compara con `rolPaso.idRol` de los pasos.
- `nombreRol` (nuevo): nombre visible del rol; es lo que se debe mostrar.
- `codigo`: código estable, o `null` (la llave siempre está) en un rol sin código.

Con Lucía (Enfermera):

```json
{
  "usuario": {"idUsuario": "de12000f-0000-4000-8000-000000000002", "idPersona": "de12000b-0000-4000-8000-000000000002",
              "nombres": "Lucía", "apellidos": "Pérez"},
  "rolActivo": {"idPersonaRol": "de12000e-0000-4000-8000-000000000003",
                "idRol": "de120001-0000-4000-8000-000000000001",
                "codigo": "ENFERMERA", "nombreRol": "Enfermera",
                "idClinica": "de120000-0000-4000-8000-000000000001", "nombreClinica": "Clínica Santa Ana"},
  "csrfToken": "…"
}
```

El frontend no debe deducir permisos del código, salvo `ADMINISTRACION` (recepción, catálogos y
personas). Lo que puede hacer cada empleado sobre un paso se decide con `rolPaso.idRol === rolActivo.idRol`.

### `GET /api/atencion/asignaciones`

Misma forma que antes; ahora incluye cualquier rol de empleado. `codigoRol` es `null` en un rol sin
código y **la llave nunca se omite**. Con Lucía:

```json
[
  {"idPersonaRol": "de12000e-0000-4000-8000-000000000003", "codigoRol": "ENFERMERA", "nombreRol": "Enfermera",
   "idClinica": "de120000-0000-4000-8000-000000000001", "nombreClinica": "Clínica Santa Ana", "activa": true}
]
```

### Dashboard y `GET /api/atencion/consultas/{id}`

- Cada paso trae **`rolPaso: {idRol, nombre}`**: el rol de la definición del paso; en un paso histórico
  sin definición, el rol de su responsable; `null` si no hay ninguno. Puede diferir de
  `responsable.rol`.
- `responsable` de un paso COMPLETADO es ahora **quien lo completó** (antes, el asignado).
- `pendienteParaMi` es `true` si hay un paso PENDIENTE cuyo rol es el del rol activo, **o** si la
  consulta está abierta, ya no tiene pendientes y el rol activo completó la finalización (le falta
  cerrarla).
- `GET /atencion/consultas/{id}` devuelve exactamente el mismo objeto que su elemento del dashboard.

### Errores nuevos

| Status | `code` | `message` | Cuándo |
| --- | --- | --- | --- |
| 403 | `consulta.noResponsablePaso` | "Este paso lo atiende el rol {rol}." (p. ej. "Este paso lo atiende el rol Enfermera.") | `finalizacion`, `retroceso`, `responsable`, `examenes` u `ordenes-examen` sobre un paso de otro rol. |
| 403 | `consulta.soloFinalizador` | "Cierra la consulta quien completó el paso de finalización." | `PUT /api/consultas/{id}/cierre` por alguien distinto de quien completó la finalización. |
| 403 | `consulta.soloEmpleado` | "Solo un empleado con rol activo puede atender consultas." | El rol activo ya no está activo o es Paciente. Reemplaza a `consulta.soloMedico`. |
| 403 | `consulta.rolClinica` | "Seleccione su rol y clínica." | La sesión no tiene rol activo. Reemplaza a `consulta.medicoClinica`. |
| 409 | `consulta.sinPasoParaRol` | "Esta consulta no tiene un paso pendiente para su rol." | `PUT /api/consultas/{id}/sesion` sin un paso pendiente de su rol y sin que le toque cerrarla. |
| 400 | `consulta.examenNoAsignable` (ya existía) | "El examen no está activo o no se puede ordenar en este paso." | Ahora también cuando el examen no tiene ningún tipo de examen activo. |

Las claves `consulta.soloMedico`, `consulta.medicoClinica` y `consulta.medicoPropio` dejaron de
existir (esta última es ahora `consulta.empleadoPropio`).

### `DELETE /api/atencion/consulta` (nuevo)

- Requiere sesión y `X-CSRF-Token`; sin cuerpo; responde **204**.
- Quita la consulta en curso de la sesión **sin cerrarla**: pasos, valores y órdenes quedan igual, y
  se puede volver a seleccionar con `PUT /api/consultas/{id}/sesion`.
- Después `GET /api/atencion` devuelve `idConsulta: null` y `PUT /api/atencion/rol-activo` ya no da
  409 `rolCambio.cerrarAntesCambiar`.
- Idempotente: sin consulta en curso también responde 204.
- Errores: 401 `auth.sesion`, 403 `auth.asignacion`, 403 `auth.csrf`.

### `GET /api/consultas-procedimientos-pasos/{id}/examenes-asignables`

Cada examen trae ahora **`tiposExamen`**: los tipos activos del examen, ordenados por nombre; es una
lista (un examen puede tener varios tipos) y nunca está vacía. Un examen sin tipo activo ya no se lista.

```json
[{"idExamen": "de120005-0000-4000-8000-000000000002", "nombre": "Glucosa en ayunas",
  "tiposExamen": [{"idTipoExamen": "de120004-0000-4000-8000-000000000001", "nombre": "Laboratorio clínico"}]},
 {"idExamen": "de120005-0000-4000-8000-000000000004", "nombre": "Rayos X de tórax",
  "tiposExamen": [{"idTipoExamen": "de120004-0000-4000-8000-000000000002", "nombre": "Imagenología"}]}]
```

### Permisos REST

- Cualquier empleado lee catálogos, personas, consultas y datos clínicos de su clínica, usa las
  acciones del flujo y crea consultas por `POST /api/personas-roles/{id}/consultas`.
- Atención al público además escribe catálogos, personas y `/api/recepcion/*`.
- Ya no hay rutas "solo DOCTOR": qué paso puede tocar cada empleado lo decide `rolPaso`.

## Datos demo y comandos para el contenedor

Detalle en `docs/datos-demo-rubrica.md` y `docs/LEVANTAR.md` §5.1 y §10. Para el contenedor
`postgres`, base `galenasv`, usuario `raul`, desde la raíz de `galenasv`:

```bash
mvn -DskipTests package
RESPALDO=respaldo-galenasv-$(date +%Y%m%d-%H%M%S).dump
docker exec postgres pg_dump -U raul -d galenasv -Fc > "$RESPALDO"
docker exec -i postgres pg_restore -l < "$RESPALDO" | head -5
docker exec -i postgres psql -X -v ON_ERROR_STOP=1 -U raul -d galenasv < docs/migrations/20261113_limpieza_demo_anterior.sql
docker exec -i postgres psql -X -v ON_ERROR_STOP=1 -U raul -d galenasv < docs/migrations/tests/datos_demo_rubrica_limpieza_test.sql
bash scripts/preparar-bd-desarrollo.sh postgres galenasv raul
docker exec -i postgres psql -X -v ON_ERROR_STOP=1 -U raul -d galenasv < docs/migrations/tests/datos_demo_rubrica_test.sql
# Para volver al respaldo:
# docker exec -i postgres pg_restore -U raul -d galenasv --clean --if-exists --single-transaction < "$RESPALDO"
```

En una base existente el DUI conserva su UUID (por ejemplo `de110004-…-1`); en una base nueva es
`de120003-…-1`. El frontend debe tomarlo siempre de `GET /api/publico/tipos-documento`.
