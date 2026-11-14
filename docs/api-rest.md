# API REST de GalenoSV

`AppConfig` activa Jakarta REST bajo `/api`. Con el WAR `galenosv.war` y la configuración local de Liberty, la base es `http://localhost:9080/galenosv/api`. Si el despliegue cambia el contexto, sustituir `/galenosv`.

## Capas

```text
AppConfig                         Ruta base y descubrimiento de @Path / @Provider
boundary/resources/*Resource      HTTP y conversión explícita entidad ↔ DTO
boundary/resources/AbstractResource CRUD y comprobación de UUID / existencia
boundary/resources/dto            Contratos JSON y Bean Validation
boundary/resources/provider       Traducción centralizada de errores
control/service                   Reglas de negocio y transacciones EJB existentes
entity/repository                 Persistencia JPA existente
```

Cada recurso usa su servicio y resuelve los UUID relacionados mediante sus servicios. El mapeo omite las colecciones JPA y convierte relaciones a identificadores: no se serializan grafos de entidades. La transacción de `AbstractResource` incluye la lectura, resolución de relaciones, escritura y construcción del DTO. Un error revierte los cambios.

## Recursos

El CRUD existente se conserva: cada colección admite GET y POST, y cada registro individual GET, PUT y DELETE. Se añade PATCH para los datos administrativos y de catálogo, y para las notas de una consulta. Las nuevas rutas anidadas y acciones usan las reglas del flujo de la app, detalladas más abajo.

| Entidad | Ruta |
| --- | --- |
| `Clinica` | `/api/clinicas` |
| `Consulta` | `/api/consultas` |
| `ConsultaProcedimiento` | `/api/consultas-procedimientos` |
| `ConsultaProcedimientoPaso` | `/api/consultas-procedimientos-pasos` |
| `Documento` | `/api/documentos` |
| `Examen` | `/api/examenes` |
| `ExamenResultado` | `/api/examenes-resultados` |
| `ExamenTipoExamen` | `/api/examenes-tipos-examen` |
| `MedioContacto` | `/api/medios-contacto` |
| `OrdenExamen` | `/api/ordenes-examen` |
| `Persona` | `/api/personas` |
| `PersonaRol` | `/api/personas-roles` |
| `Procedimiento` | `/api/procedimientos` |
| `ProcedimientoPaso` | `/api/procedimientos-pasos` |
| `ProcedimientoPasoExamen` | `/api/procedimientos-pasos-examenes` |
| `ProcedimientoPasoSecuencia` | `/api/procedimientos-pasos-secuencias` |
| `Rol` | `/api/roles` |
| `TipoDocumento` | `/api/tipos-documento` |
| `TipoExamen` | `/api/tipos-examen` |
| `TipoMedioContacto` | `/api/tipos-medio-contacto` |

`GET /api/jakartaee11` devuelve `{"message":"ping Jakarta EE"}` para comprobar que REST responde.

## Autenticación y rutas públicas

Detalle completo en `docs/autenticacion.md`. Resumen del contrato (HU1, HU2):

| Operación | Sesión | Cuerpo / respuesta |
| --- | --- | --- |
| `GET /api/publico/tipos-documento` | No | 200 con `[{idTipoDocumento, nombre, indicaciones, expresionRegular}]`, solo tipos activos, ordenados por nombre. `indicaciones` y `expresionRegular` pueden ser `null`. |
| `POST /api/auth/login` | No (exige `Origin` permitido) | `{"idTipoDocumento":"UUID","numeroDocumento":"00000000-1"}` → 204 y cookie nueva. |
| `GET /api/auth/me` | Sí | Usuario, rol activo y `csrfToken`. |
| `POST /api/auth/logout` | Sí + `X-CSRF-Token` | 204. |

Errores de `POST /api/auth/login`:

| Código | Caso |
| --- | --- |
| 400 `json.invalido` / validación | `idTipoDocumento` no es UUID; número vacío, mayor de 50 caracteres o con saltos de línea. |
| 401 `auth.credenciales` | Mismo mensaje para documento inexistente, documento de otro tipo, tipo inactivo, usuario inactivo, persona sin asignación activa y persona que solo es PACIENTE. |
| 403 `auth.origen` | `Origin`/`Referer` no permitido. |
| 429 `auth.demasiadosIntentos` | Más de diez intentos por dirección en cinco minutos. |

Las rutas bajo `/api/publico/` no requieren sesión ni CSRF y nunca devuelven notas, valores clínicos ni resultados. Cualquier ruta con un segmento `.` o `..` devuelve 400 `ruta.invalida`.

### Asignaciones y rol activo

| Operación | Sesión | Cuerpo / respuesta |
| --- | --- | --- |
| `GET /api/atencion/asignaciones` | Sí (sin CSRF por ser GET) | 200 con `[{idPersonaRol, codigoRol, nombreRol, idClinica, nombreClinica, activa}]`. |
| `PUT /api/atencion/rol-activo` | Sí + `X-CSRF-Token` | `{"idPersonaRol":"UUID"}` → 200 con `{idPersonaRol, idConsulta}` y nuevo identificador de sesión. |

`GET /api/atencion/asignaciones` devuelve todas las asignaciones de empleado activas
(cualquier rol activo excepto `PACIENTE`, en una clínica activa; B8) de la cuenta autenticada, en todas sus
clínicas. Usa `ContextoRolService.listar()` y conserva el orden de
`UsuarioRepository.asignaciones()`: fecha de creación de la asignación y luego `idPersonaRol`.
`activa` es `true` solo en la asignación que es el rol activo de la sesión. Después del login
es la primera asignación de empleado. Si la sesión no tiene rol activo, todas salen en `false`.
No incluye datos de la persona ni asignaciones PACIENTE. `codigoRol` es `null` (la clave
siempre está) en un rol creado sin código; el frontend debe mostrar `nombreRol`.

```json
[
  {"idPersonaRol":"…","codigoRol":"DOCTOR","nombreRol":"Doctor","idClinica":"…","nombreClinica":"Clínica de demostración","activa":true},
  {"idPersonaRol":"…","codigoRol":"ADMINISTRACION","nombreRol":"Administración","idClinica":"…","nombreClinica":"Clínica de demostración","activa":false}
]
```

`PermisosApiFilter` no limita `/atencion`, así que el recurso exige la sesión por sí mismo.
Esto se suma a `AutenticacionFilter`.

Errores:

| Ruta | Código | Caso |
| --- | --- | --- |
| `GET asignaciones` | 401 `auth.sesion` | Sin sesión, sesión vencida o usuario inactivo. |
| `PUT rol-activo` | 400 `validacion.invalida` | Falta `idPersonaRol` o no es UUID. |
| `PUT rol-activo` | 401 `auth.sesion` | Sin sesión o usuario inactivo. |
| `PUT rol-activo` | 403 `auth.csrf` | Falta el token CSRF o no coincide. |
| `PUT rol-activo` | 403 `auth.asignacion` | La asignación no es una asignación de empleado activa de la cuenta. |
| `PUT rol-activo` | 409 `rolCambio.cerrarAntesCambiar` | Hay una consulta en curso y se pide otra asignación. Primero se cierra la consulta; no se renueva la sesión. Es un conflicto de estado, igual que `consulta.cerrarAntesCrear`. |

### Dashboard de consultas (HU3)

| Operación | Sesión | Respuesta |
| --- | --- | --- |
| `GET /api/atencion/dashboard?desde=yyyy-MM-dd&hasta=yyyy-MM-dd` | Sí (sin CSRF por ser GET) | 200 con las consultas de la clínica del rol activo, en orden ascendente por `fechaInicio` y luego `idConsulta`. |
| `GET /api/atencion/dashboard/pasos/{idConsultaProcedimientoPaso}/valor` | Sí | 200 con `{idConsultaProcedimientoPaso, nombre, estado, fechaFin, valor}`. |
| `GET /api/atencion/consultas/{idConsulta}` | Sí | 200 con **una** consulta, con el mismo JSON que su elemento del dashboard (HU3/HU4). |

- **Roles:** cualquier rol de empleado (activo y distinto de PACIENTE). La clínica es la de la
  asignación activa, leída de la base. Ningún parámetro del cliente la cambia.
- **Sesión:** `PermisosApiFilter` no revisa `/atencion`, así que `DashboardService` comprueba,
  antes de cualquier consulta, que haya sesión vigente y un rol activo, y que ese rol siga siendo
  una asignación de empleado activa de la cuenta. No usa `AtencionService.historial`, que es de
  la vista JSF.
- **Fechas:** son días de El Salvador, con `hasta` incluido.
  - Sin `desde` ni `hasta` se usa hoy. Con uno solo, se usa ese día en los dos extremos.
  - El rango admite como máximo 31 días, contando ambos extremos.
- **Una consulta:** `GET /api/atencion/consultas/{idConsulta}` devuelve la consulta seleccionada
  con el mismo JSON, llave por llave, que su elemento en el dashboard, con las mismas validaciones
  de sesión y clínica y los mismos roles. No tiene límite de fechas: sirve para consultas pasadas
  y futuras. Solo lee; para atenderla se selecciona con `PUT /api/consultas/{id}/sesion`.
- **Fechas devueltas:** van con offset `-06:00`. `fechaNacimiento` es el día en El Salvador,
  como en la vista JSF de personas.

```json
[{
  "idConsulta": "…", "fechaInicio": "2026-11-13T08:00:00-06:00", "fechaFin": null,
  "pendienteParaMi": true,
  "paciente": {"nombres": "…", "apellidos": "…", "fechaNacimiento": "1999-12-31", "edad": 26,
               "documentos": [{"tipoDocumento": "DUI", "numero": "00000000-3"}]},
  "procedimientos": [{
    "idConsultaProcedimiento": "…", "nombre": "Consulta general de demostración",
    "fechaInicio": "…", "fechaFin": null,
    "pasos": [{"idConsultaProcedimientoPaso": "…", "nombre": "Toma de signos vitales", "orden": 1, "nivel": 0,
               "estado": "COMPLETADO", "responsable": {"nombre": "…", "rol": "Doctor"},
               "fechaInicio": "…", "fechaFin": "…", "tieneValor": true}]
  }]
}]
```

**Campos de la consulta y el paciente**

- `fechaFin` de la consulta es `null` mientras está abierta. Para atenderla, se selecciona con
  `PUT /api/consultas/{id}/sesion`.
- `pendienteParaMi` es `true` en dos casos:
  - la consulta tiene un paso PENDIENTE cuyo rol es el del rol activo. Se toma el rol de la
    definición del paso y, si el paso no tiene definición, el rol de su responsable;
  - (B8) la consulta está abierta, no tiene pasos pendientes y el último paso de finalización
    completado lo completó la asignación activa: le falta cerrarla con `PUT /api/consultas/{id}/cierre`.

  Coincide con lo que acepta `PUT /api/consultas/{id}/sesion`, salvo una consulta sin pasos, que
  cualquier empleado puede seleccionar pero no sale como pendiente.
- `edad` son los años cumplidos a hoy, y es `null` si no hay fecha de nacimiento.
- `documentos` lleva todos los documentos del paciente, ordenados por tipo, sin `rutaFisica`.

**Campos del procedimiento y los pasos**

- Un procedimiento sin pasos aparece con `pasos: []`.
- Los pasos siguen el grafo de secuencias del catálogo:
  - `nivel` es la profundidad del paso (0 para los iniciales) y sirve para la jerarquía.
  - `orden` es la posición dentro del procedimiento. Los empates de nivel se resuelven por
    `fechaInicio`.
  - Un paso sin definición de catálogo va al final, con `nivel` 0.
- `responsable` es la asignación del paso: en uno COMPLETADO, quién lo realizó; en uno
  PENDIENTE, quién debe realizarlo. Es `null` si no hay responsable.
- `tieneValor` indica si el paso tiene un valor registrado. El valor no viaja en el dashboard:
  se pide con la ruta `…/valor` cuando el usuario abre el paso.
- No se envían `observaciones`, `referenciaExterna`, órdenes de examen ni resultados.

Sin N+1: la respuesta usa 4 consultas a la base para todo el rango (o para la consulta de
`atencion/consultas/{idConsulta}`): consultas, pasos (con
procedimientos), documentos y secuencias. Son proyecciones, nunca entidades, así que el valor
de los pasos no se carga. A esas se suman las 2 de la validación de la asignación.

Errores:

| Código | Caso |
| --- | --- |
| 400 `dashboard.fecha` | `desde` o `hasta` no tiene el formato `yyyy-MM-dd`. |
| 400 `dashboard.rangoInvertido` | `desde` es posterior a `hasta`. |
| 400 `dashboard.rangoExcedido` | Más de 31 días. |
| 401 `auth.sesion` | Sin sesión, sesión vencida o usuario inactivo. |
| 403 `auth.rolActivo` | La sesión no tiene un rol activo. |
| 403 `auth.asignacion` | El rol activo ya no es una asignación de empleado activa de la cuenta. |
| 404 `consulta.noExiste` | Ruta `atencion/consultas/{idConsulta}`: la consulta no existe o es de otra clínica (misma respuesta). Un id que no es UUID da 404 de la ruta. |
| 404 `consulta.pasoNoExiste` | Ruta `…/valor`: el paso no existe o es de una consulta de otra clínica (misma respuesta). Un id que no es UUID da 404 de la ruta. |

### Búsqueda pública de citas (HU1)

Las tres rutas son `POST` con cuerpo JSON: el número de documento nunca viaja en la URL. Como toda escritura, exigen un `Origin` (o `Referer`) permitido, pero no sesión ni CSRF.

| Operación | Cuerpo | Respuesta |
| --- | --- | --- |
| `POST /api/publico/clinicas` | `{idTipoDocumento, numeroDocumento}` | `200 [{idClinica, nombre}]`: clínicas activas donde el dueño del documento es PACIENTE, ordenadas por nombre. |
| `POST /api/publico/citas` | `{idTipoDocumento, numeroDocumento, idClinica, desde, hasta}` | `200 [{idConsulta, fechaInicio, nombreClinica, procedimientos: [String]}]` ordenado por `fechaInicio`. |
| `POST /api/publico/citas/{idConsulta}/ticket` | `{idTipoDocumento, numeroDocumento}` | `200 application/pdf` con `Content-Disposition: attachment; filename="ticket-cita-<8 primeros>.pdf"`. |

- `desde` y `hasta` son fechas `yyyy-MM-dd` en hora de El Salvador; `hasta` se incluye completo.
- Rango: se rechaza con 400 `citas.rango` si `hasta < desde` o si `ChronoUnit.DAYS.between(desde, hasta) > 366`, es decir, se compara la **diferencia** entre fechas, no los días contados con ambos extremos. `hasta = desde + 366 días` se acepta (367 días calendario, contando ambos extremos); `desde + 367 días` da 400. Ejemplo: `2026-01-01`→`2027-01-02` se acepta; `2026-01-01`→`2027-01-03` no. `desde = hasta` es válido (un día). Este límite no es el del dashboard, que cuenta ambos extremos.
- `fechaInicio` va en ISO-8601 con offset de El Salvador, p. ej. `2026-11-12T08:00:00-06:00`.
- Nunca se devuelven `observaciones`, `referenciaExterna`, `valor`, estados, responsables, órdenes ni resultados, ni tampoco nombre o documento del paciente. El PDF contiene lo mismo que la card: código, fecha y hora, clínica y procedimientos.
- `numeroDocumento`: obligatorio, hasta 50 caracteres, sin saltos de línea. Se compara exacto después de recortar espacios.

Errores de validación del cuerpo (las tres rutas `POST`):

| Error | Cuándo |
| --- | --- |
| 400 `json.invalido` | Cuerpo ausente o `null`. `details` va vacío. |
| 400 `validacion.invalida` | Falla alguna restricción del cuerpo. `details` trae un elemento `{field, message}` por cada restricción violada, ordenados por `field` y luego por `message`. |
| 400 `citas.rango` | Solo `citas`: el cuerpo es válido pero el rango no (ver arriba). `details` va vacío. |

Restricciones que producen `validacion.invalida` (el `field` es el nombre de la propiedad del cuerpo, sin prefijos):

| `field` | Rutas | Restricción |
| --- | --- | --- |
| `idTipoDocumento` | las tres | obligatorio (`@NotNull`) |
| `numeroDocumento` | las tres | obligatorio y no en blanco (`@NotBlank`), máximo 50 caracteres (`@Size`), sin `\r` ni `\n` (`@Pattern`) |
| `idClinica` | `citas` | obligatorio (`@NotNull`) |
| `desde`, `hasta` | `citas` | obligatorios (`@NotNull`) |

- Un mismo campo puede aparecer varias veces en `details` si viola varias restricciones. `message` es el texto de Bean Validation (puede depender del idioma del servidor); el frontend debe guiarse por `field`, no por `message`. Nunca se copia el valor rechazado.
- La validación ocurre antes de cualquier consulta: en `ticket`, un cuerpo inválido da 400 aunque `idConsulta` no sea UUID o no exista. En `citas`, `citas.rango` solo se evalúa si no hubo `validacion.invalida`.
- Un cuerpo inválido también cuenta para el límite de solicitudes.

```json
{
  "status": 400,
  "code": "validacion.invalida",
  "message": "Los datos de la solicitud no son válidos.",
  "path": "/galenosv/api/publico/citas",
  "timestamp": "2026-11-13T17:00:00Z",
  "details": [
    {"field": "idClinica", "message": "no debe ser nulo"},
    {"field": "numeroDocumento", "message": "no debe estar vacío"}
  ]
}
```

Respuestas iguales, para no revelar qué documentos existen:

| Ruta | Documento inexistente, de otro tipo o con tipo inactivo | Documento válido sin datos |
| --- | --- | --- |
| `clinicas` | `200 []` | `200 []` |
| `citas` | `200 []` | `200 []` (sin citas en el rango o clínica donde no es paciente) |
| `ticket` | `404 cita.noExiste` | `404 cita.noExiste` (cita ajena, inexistente o id inválido) |

Límite: las tres rutas comparten **60 solicitudes por IP cada 5 minutos** (`IntentosAcceso.Ambito.BUSQUEDA_PUBLICA`), separadas de las 10 del login. Cada solicitud cuenta antes de validar el cuerpo. Al superarlo: `429 publico.demasiadasSolicitudes` con `Retry-After: 300`. `GET /api/publico/tipos-documento` no cuenta.

Ejemplo:

```http
POST /galenosv/api/publico/citas
Origin: http://localhost:9080
Content-Type: application/json

{"idTipoDocumento":"de110004-0000-4000-8000-000000000001","numeroDocumento":"00000000-3",
 "idClinica":"de110000-0000-4000-8000-000000000001","desde":"2026-11-01","hasta":"2026-11-30"}
```

```json
[{"idConsulta":"de110009-0000-4000-8000-000000000002","fechaInicio":"2026-11-12T08:00:00-06:00",
  "nombreClinica":"Clínica de demostración","procedimientos":["Consulta general de demostración"]}]
```

## Filtros y colecciones relacionadas

Los listados admiten filtros por los identificadores de sus relaciones. Se ejecutan en la base de datos y pueden combinarse mediante `AND`. Cada parámetro se envía una sola vez y contiene un UUID canónico. Los filtros desconocidos, repetidos, vacíos o con UUID inválidos devuelven 400. Sin filtros se conserva el listado completo; sin coincidencias se devuelve 200 con `[]`, incluso si el UUID usado como filtro no corresponde a un registro existente.

| Recurso | Filtros UUID permitidos |
| --- | --- |
| `/api/consultas` | `idPersonaRol`, `idPersona`, `idClinica`, `idRol` |
| `/api/consultas-procedimientos` | `idConsulta`, `idProcedimiento` |
| `/api/consultas-procedimientos-pasos` | `idConsultaProcedimiento`, `idPersonaRol`, `idProcedimientoPaso` |
| `/api/documentos` | `idPersona`, `idTipoDocumento` |
| `/api/examenes` | `idTipoExamen` |
| `/api/examenes-resultados` | `idOrdenExamen` |
| `/api/examenes-tipos-examen` | `idExamen`, `idTipoExamen` |
| `/api/medios-contacto` | `idPersona`, `idTipoMedioContacto` |
| `/api/ordenes-examen` | `idConsultaProcedimientoPaso` |
| `/api/personas` | `idClinica`, `idRol` |
| `/api/personas-roles` | `idPersona`, `idRol`, `idClinica` |
| `/api/procedimientos-pasos` | `idProcedimiento`, `idRol` |
| `/api/procedimientos-pasos-examenes` | `idProcedimientoPaso`, `idExamen` |
| `/api/procedimientos-pasos-secuencias` | `idProcedimientoPaso`, `idProcedimientoPasoReferencia` |

Las personas pertenecen a una clínica mediante `PersonaRol`. Filtrar personas por `idClinica` incluye las personas con al menos una asignación en esa clínica y devuelve cada persona una sola vez. Al combinar `idClinica` e `idRol`, ambos deben coincidir en la misma asignación.

```text
GET /api/personas?idClinica=UUID_DE_CLINICA
GET /api/personas?idClinica=UUID_DE_CLINICA&idRol=UUID_DE_ROL
GET /api/documentos?idPersona=UUID_DE_PERSONA&idTipoDocumento=UUID_DE_TIPO
GET /api/consultas?idPersona=UUID_DE_PERSONA&idClinica=UUID_DE_CLINICA
```

También se pueden listar las colecciones de un recurso mediante rutas anidadas. Las rutas conservan los nombres en plural de la API. `{id}` es el UUID del padre; se comprueba su existencia antes de consultar la colección. Un padre inexistente devuelve 404 y un UUID inválido devuelve 400. Si el padre existe y no tiene registros relacionados, se devuelve 200 con `[]`.

| Ruta GET | DTO de cada resultado |
| --- | --- |
| `/api/clinicas/{id}/personas` | `PersonaDto` |
| `/api/clinicas/{id}/personas-roles` | `PersonaRolDto` |
| `/api/clinicas/{id}/consultas` | `ConsultaDto` |
| `/api/consultas/{id}/procedimientos` | `ConsultaProcedimientoDto` |
| `/api/consultas-procedimientos/{id}/pasos` | `ConsultaProcedimientoPasoDto` |
| `/api/consultas-procedimientos-pasos/{id}/ordenes-examen` | `OrdenExamenDto` |
| `/api/examenes/{id}/clasificaciones` | `ExamenTipoExamenDto` |
| `/api/ordenes-examen/{id}/resultados` | `ExamenResultadoDto` |
| `/api/personas/{id}/documentos` | `DocumentoDto` |
| `/api/personas/{id}/medios-contacto` | `MedioContactoDto` |
| `/api/personas/{id}/personas-roles` | `PersonaRolDto` |
| `/api/personas/{id}/consultas` | `ConsultaDto` |
| `/api/personas-roles/{id}/consultas` | `ConsultaDto` |
| `/api/personas-roles/{id}/pasos-consulta` | `ConsultaProcedimientoPasoDto`. **Restringida**: 403, ver nota. |
| `/api/procedimientos/{id}/pasos` | `ProcedimientoPasoDto` |
| `/api/procedimientos/{id}/consultas-procedimientos` | `ConsultaProcedimientoDto`. **Restringida**: 403, ver nota. |
| `/api/procedimientos-pasos/{id}/examenes-configurados` | `ProcedimientoPasoExamenDto` |
| `/api/procedimientos-pasos/{id}/secuencias` | `ProcedimientoPasoSecuenciaDto` |
| `/api/procedimientos-pasos/{id}/pasos-consulta` | `ConsultaProcedimientoPasoDto`. **Restringida**: 403, ver nota. |
| `/api/roles/{id}/personas` | `PersonaDto` |
| `/api/roles/{id}/personas-roles` | `PersonaRolDto` |
| `/api/roles/{id}/procedimientos-pasos` | `ProcedimientoPasoDto` |
| `/api/tipos-documento/{id}/documentos` | `DocumentoDto`. **Restringida**: 403, ver nota. |
| `/api/tipos-examen/{id}/examenes` | `ExamenDto` |
| `/api/tipos-examen/{id}/clasificaciones` | `ExamenTipoExamenDto` |
| `/api/tipos-medio-contacto/{id}/medios-contacto` | `MedioContactoDto`. **Restringida**: 403, ver nota. |

**Rutas restringidas.** Las 5 rutas marcadas existen, pero `PermisosApiFilter` responde 403
`auth.permiso`. Se verificó con DOCTOR; por cómo está escrito el filtro, la respuesta es la misma
para cualquier rol. No se quitan porque el recurso las define. Son 403 porque listarían registros
de otras consultas, personas o clínicas: el filtro solo deja leer estas colecciones cuando el
padre identifica a su dueño.

| Ruta restringida | Motivo | Alternativa |
| --- | --- | --- |
| `/api/personas-roles/{id}/pasos-consulta` | El filtro no reconoce el segmento `pasos-consulta`. | `/api/consultas-procedimientos/{id}/pasos` |
| `/api/procedimientos-pasos/{id}/pasos-consulta` | Igual que la anterior. | `/api/consultas-procedimientos/{id}/pasos` |
| `/api/procedimientos/{id}/consultas-procedimientos` | El padre es un catálogo; exige `idConsulta`. | `/api/consultas/{id}/procedimientos` |
| `/api/tipos-documento/{id}/documentos` | El padre es un catálogo; exige `idPersona`. | `/api/personas/{id}/documentos` |
| `/api/tipos-medio-contacto/{id}/medios-contacto` | El padre es un catálogo; exige `idPersona`. | `/api/personas/{id}/medios-contacto` |

La colección `secuencias` de un paso lista sus transiciones salientes, usando `idProcedimientoPaso`; `idProcedimientoPasoReferencia` identifica el destino. Las rutas de procedimientos de consulta, clasificaciones y exámenes configurados devuelven los DTO de las asignaciones o ejecuciones indicados en la tabla.

Los filtros del recurso hijo también se admiten en una ruta anidada. El identificador del padre queda fijado por la URL; reenviarlo con otro valor devuelve 400 con `filtro.inconsistente`.

```text
GET /api/personas/UUID_DE_PERSONA/documentos
GET /api/personas/UUID_DE_PERSONA/documentos?idTipoDocumento=UUID_DE_TIPO
GET /api/clinicas/UUID_DE_CLINICA/personas?idRol=UUID_DE_ROL
GET /api/consultas/UUID_DE_CONSULTA/procedimientos
GET /api/ordenes-examen/UUID_DE_ORDEN/resultados
```

## Escrituras relacionadas y cambios parciales

Las rutas anidadas admiten operaciones según el flujo de la app. Cada colección permite `GET`; cada hijo puede consultarse con `GET /coleccion/{idHijo}`. La tabla indica las escrituras habilitadas. `HEAD` devuelve las cabeceras sin cuerpo y `OPTIONS` informa los métodos efectivos mediante `Allow`. Las operaciones no habilitadas devuelven 405.

| Colección | Métodos sobre la colección | Métodos sobre `/{idHijo}` |
| --- | --- | --- |
| `/api/clinicas/{id}/personas` | GET, POST | GET |
| `/api/clinicas/{id}/personas-roles` | GET, POST | GET, PUT, PATCH, DELETE |
| `/api/personas/{id}/documentos` | GET, POST | GET, PUT, PATCH, DELETE |
| `/api/personas/{id}/medios-contacto` | GET, POST | GET, PUT, PATCH, DELETE |
| `/api/personas/{id}/personas-roles` | GET, POST | GET, PUT, PATCH, DELETE |
| `/api/roles/{id}/personas-roles` | GET, POST | GET, PUT, PATCH, DELETE |
| `/api/personas-roles/{id}/consultas` | GET, POST | GET, PUT, PATCH |
| `/api/consultas/{id}/procedimientos` | GET, POST | GET |
| `/api/consultas-procedimientos-pasos/{id}/ordenes-examen` | GET, POST | GET |
| `/api/ordenes-examen/{id}/resultados` | GET, POST | GET |
| `/api/procedimientos/{id}/pasos` | GET, POST | GET |
| `/api/procedimientos-pasos/{id}/examenes-configurados` | GET, POST | GET |
| `/api/examenes/{id}/clasificaciones` | GET, POST | GET, PUT, PATCH, DELETE |
| `/api/tipos-examen/{id}/clasificaciones` | GET, POST | GET, PUT, PATCH, DELETE |

Las demás colecciones de la tabla de lectura permiten consultar la colección y sus hijos. Una persona creada con `POST /api/clinicas/{id}/personas` se guarda junto con su pertenencia a esa clínica, sin rol, mediante `RegistroPersonaService`. Para asignar un rol, crear o editar un registro de `personas-roles`. La persona de una asignación existente se conserva en las nuevas escrituras. El rol es opcional; la clínica y la persona son obligatorias y la clínica y el rol seleccionado deben estar activos en las nuevas escrituras. Al editar una asignación que ya tiene rol, omitirlo o enviar null lo conserva, como en la ficha de Personas. Eliminar una asignación conserva la persona; eliminar una persona sigue las restricciones de su historial.

En `POST` y `PUT` anidados puede omitirse el UUID del padre en el cuerpo: la URL lo fija antes de validar el DTO. Si se envía otro valor, la respuesta es 400 con `relacion.inconsistente`. Antes de leer, editar o eliminar un hijo se comprueba en el repositorio que pertenezca al padre; un hijo ajeno devuelve 404. La eliminación de documentos y contactos de una persona usa los mismos métodos de propiedad que la ficha de Personas. Las nuevas escrituras también exigen un tipo activo, validan su expresión de formato y recortan espacios del valor; se mantiene la validación de DUI único del servicio.

`PUT` sustituye los campos editables y requiere el DTO completo, salvo el identificador del padre fijado por la URL. `PATCH` aplica JSON Merge Patch con `Content-Type: application/merge-patch+json` o `application/json`: omitir un campo lo conserva y enviar `null` elimina un opcional. El resultado completo se valida antes de persistir; una asignación con rol conserva ese rol cuando se omite o se envía null. No se puede cambiar el UUID propio o el padre de la URL; eliminar un campo obligatorio o enviar un campo desconocido devuelve 400. Los filtros de consulta se usan para lectura; las escrituras se dirigen a registros individuales.

```text
POST /api/personas/UUID_DE_PERSONA/documentos
{"valor":"12345678-9","idTipoDocumento":"UUID_DEL_TIPO"}

PATCH /api/personas/UUID_DE_PERSONA/documentos/UUID_DEL_DOCUMENTO
{"valor":"87654321-0","rutaFisica":null}

DELETE /api/personas/UUID_DE_PERSONA/documentos/UUID_DEL_DOCUMENTO
```

`POST /api/procedimientos/{id}/pasos` usa `ConfiguracionProcedimientoService.paso` y admite `idPasoPrevio` opcional. Verifica el rol activo y que el predecesor pertenezca al procedimiento y no sea final; crea el paso y su secuencia `SIGUIENTE` en una transacción. Las secuencias anidadas se consultan sin escrituras manuales. `idPasoPrevio` se utiliza solo en este POST. Asociar un examen desde `examenes-configurados` usa el servicio de configuración, verifica que el examen esté activo y rechaza duplicados; fecha y estado se fijan en el servidor.

## Nuevas operaciones del flujo clínico

El CRUD directo anterior de las entidades se conserva. Para seguir el comportamiento de las vistas, usar las nuevas escrituras anidadas y las acciones de esta sección. Estas operaciones usan `AtencionService`, igual que las vistas de Consulta e Historial. Requieren el rol médico y la clínica de la misma sesión HTTP. El cliente REST debe conservar las cookies de esa sesión. `GET /api/atencion` informa `idPersonaRol` e `idConsulta`; `PUT /api/atencion/rol-activo` selecciona la asignación con el mismo servicio de sesión usado por Cambiar de rol.

| Ruta del flujo | Escrituras nuevas habilitadas |
| --- | --- |
| POST /api/personas-roles/{id}/consultas | Crea una atención para un rol Paciente activo. PUT y PATCH del hijo editan sus notas; PATCH /api/consultas/{id} también usa el flujo. |
| POST /api/consultas/{id}/procedimientos | Inicia el procedimiento y genera los pasos iniciales después de comprobar todo el personal requerido. |
| /api/consultas-procedimientos/{id}/pasos | Los pasos se generan por el flujo; no hay escrituras genéricas en esta colección. |
| POST /api/consultas-procedimientos-pasos/{id}/ordenes-examen | Crea la orden de un examen asignable en un paso pendiente de la consulta (HU4). Exige `idExamen`. |
| POST /api/ordenes-examen/{id}/resultados | Registra un resultado de una orden de la consulta, incluso después del cierre. |

Las nuevas rutas del flujo no habilitan DELETE del historial ni reemplazo genérico de procedimientos ejecutados, pasos, órdenes o resultados. En estas rutas las fechas y los estados se generan en el servidor: no enviar fechas en POST; el paciente y las fechas se conservan al editar las notas de una consulta. El CRUD directo anterior mantiene su contrato. Para las transiciones se usan estas operaciones:

| Método y ruta | Cuerpo | Comportamiento |
| --- | --- | --- |
| `PUT /api/atencion/rol-activo` | `idPersonaRol` | Selecciona rol y clínica; conserva la restricción de cambio de rol durante una atención. |
| `PUT /api/consultas/{id}/sesion` | Sin cuerpo | Retoma una consulta abierta de la clínica activa que tenga un paso pendiente del rol activo, o que este rol deba cerrar (B8). |
| `PUT /api/consultas/{id}/cierre` | `referenciaExterna`, `observaciones` opcionales | Cierra cuando los pasos están completos y cada procedimiento llega a un paso final; libera la consulta en sesión. |
| `PUT /api/consultas-procedimientos-pasos/{id}/responsable` | `idPersonaRol` | Cambia el responsable de un paso pendiente, respetando rol activo y clínica. |
| `PUT /api/consultas-procedimientos-pasos/{id}/finalizacion` | `valor` opcional | Completa el paso y genera sus sucesores cuando se cumplen los predecesores. |
| `POST /api/consultas-procedimientos-pasos/{id}/examenes` | `nombre`, `idTipoExamen`, `observaciones` opcionales | Crea examen, clasificación y asociación al paso en una transacción. |
| `PUT /api/consultas-procedimientos-pasos/{id}/retroceso` | Sin cuerpo | HU4: elimina este paso, si está vacío, y reabre el anterior. Responde 200 con el paso anterior reabierto. |
| `GET /api/consultas-procedimientos-pasos/{id}/examenes-asignables` | — | HU4: exámenes que se pueden ordenar en el paso. Ver abajo. |

### Permisos por rol (B8)

`PermisosApiFilter` distingue tres casos por el código del rol activo:

| Rol activo | Qué deja pasar el filtro |
| --- | --- |
| Empleado: cualquier rol activo distinto de `PACIENTE`, incluso sin código (Atención al público, Enfermera, Doctor…) | Lectura de catálogos y de personas, consultas y datos clínicos **de su clínica**; las acciones del flujo (`sesion`, `cierre`, `finalizacion`, `retroceso`, `responsable`, `examenes`, `examenes-asignables`, órdenes y resultados); crear consultas por `POST /api/personas-roles/{id}/consultas`. |
| `ADMINISTRACION` (nombre visible "Atención al público") | Lo anterior, más la escritura de catálogos, de personas y `/api/recepcion/*`. |
| `PACIENTE` | Solo su expediente (no puede iniciar sesión desde la HU2). |

El filtro no decide qué rol atiende cada paso: eso lo valida AtencionService con el rol
responsable del paso. Una ruta de otra clínica sigue dando 403 `auth.permiso`.

Reglas de AtencionService (detalle en `docs/flujo-consulta.md`, "Quién atiende cada paso"):

| Status y código | Mensaje | Cuándo |
| --- | --- | --- |
| 403 `consulta.noResponsablePaso` | "Este paso lo atiende el rol {rol}." | `finalizacion`, `retroceso`, `responsable`, `examenes` u `ordenes-examen` sobre un paso cuyo rol responsable no es el rol activo. |
| 403 `consulta.soloFinalizador` | "Cierra la consulta quien completó el paso de finalización." | `PUT /api/consultas/{id}/cierre` hecho por alguien distinto de quien completó el último paso de finalización. |
| 403 `consulta.soloEmpleado` | "Solo un empleado con rol activo puede atender consultas." | El rol activo ya no está activo, o es PACIENTE. |
| 403 `consulta.rolClinica` | "Seleccione su rol y clínica." | La sesión no tiene rol activo. |
| 409 `consulta.sinPasoParaRol` | "Esta consulta no tiene un paso pendiente para su rol." | `PUT /api/consultas/{id}/sesion` sin un paso pendiente del rol activo y sin que le toque cerrarla. |

Al completar un paso (`finalizacion`), su responsable pasa a ser el rol activo de quien lo
completó; así el dashboard muestra quién lo realizó.

Las acciones admiten el cuerpo vacío aunque lleguen con `Content-Type: application/json`: un
cuerpo vacío o en blanco se trata como `{}`. Así, `…/retroceso` y `…/sesion` no lo usan,
`…/finalizacion` completa el paso sin valor y `…/cierre` conserva la referencia y las
observaciones. Un cuerpo que no es un objeto JSON sigue dando 400 `json.invalido`.

**Retroceso (HU4).** Sobre un paso de la clínica del rol activo y de una consulta abierta; un
paso de otra clínica recibe 403 `auth.permiso`. Quién puede hacerlo lo decide AtencionService
(ver "Permisos por rol"). También funciona en la ruta
anidada `/api/consultas-procedimientos/{cp}/pasos/{id}/retroceso`.

Condiciones:
- el paso está PENDIENTE;
- tiene un único paso anterior por secuencia `SIGUIENTE`, y ese anterior está COMPLETADO;
- el paso no tiene valor ni órdenes de examen.

Si se cumplen, en una sola transacción se borra el paso y el anterior queda PENDIENTE con
`fechaFin` null, **conservando su `valor` y su responsable**. Si algo falla, no cambia nada.
El campo `message` explica el motivo, para mostrarlo en la vista.

| Código | Caso |
| --- | --- |
| 409 `consulta.retrocesoPrimerPaso` | El paso no tiene un paso anterior: es el primero del procedimiento. |
| 409 `consulta.retrocesoConRegistros` | El paso actual tiene `valor` (no en blanco) u órdenes de examen. |
| 409 `consulta.retrocesoAnteriorPendiente` | El paso anterior no se ejecutó o no está COMPLETADO. |
| 409 `consulta.retrocesoRamificado` | El paso tiene varios anteriores, o del anterior salió otro paso además de este. |
| 409 `consulta.pasoYaCompletado` | El paso actual ya está completado. |
| 409 `consulta.yaCerrada` | La consulta está cerrada. |
| 400 `consulta.pasoSinDefinicionHistorica` | El paso no tiene definición de catálogo. |

Después del retroceso, `PUT …/finalizacion` sobre el paso reabierto **reemplaza** su valor con
el que se envíe; si se envía vacío o null, el valor se borra. Para conservarlo, la vista debe
precargarlo y reenviarlo. Al completarlo, el paso siguiente se crea de nuevo con un id nuevo y
el responsable automático.

**Exámenes asignables y órdenes con examen (HU4).** Sobre un paso de la clínica del rol activo;
un paso de otra clínica recibe 403 `auth.permiso`. Quién puede ordenar lo decide AtencionService
(ver "Permisos por rol"). Las dos rutas
también funcionan anidadas bajo `/api/consultas-procedimientos/{cp}/pasos/{id}`.

Un examen es **asignable** a un paso si está activo y tiene una asociación activa
(`procedimientos-pasos/{id}/examenes-configurados`) con la definición de catálogo del paso.

```text
GET /api/consultas-procedimientos-pasos/UUID_DEL_PASO/examenes-asignables
200 [{"idExamen":"…","nombre":"Glucosa en ayunas"},{"idExamen":"…","nombre":"Hemograma completo"}]
```

La lista viene ordenada por `nombre` y luego por `idExamen`. Un paso sin definición, o sin
asociaciones activas, da `[]`. Como solo lee, se puede consultar en cualquier estado del paso y de
la consulta.

```text
POST /api/consultas-procedimientos-pasos/UUID_DEL_PASO/ordenes-examen
{"idExamen":"UUID_DEL_EXAMEN","indicaciones":"En ayunas"}
201 {"idOrdenExamen":"…","idExamen":"…","idConsultaProcedimientoPaso":"UUID_DEL_PASO","indicaciones":"En ayunas","fechaCreacion":"…"}
```

El paso se toma de la URL: `idConsultaProcedimientoPaso` puede omitirse y, si se envía otro, la
respuesta es 400 `relacion.inconsistente`. `indicaciones` es opcional (hasta 255 caracteres, se
recorta). La consulta debe estar abierta y el paso PENDIENTE.

| Código | Caso |
| --- | --- |
| 400 `consulta.examenObligatorio` | Falta `idExamen`. |
| 400 `consulta.examenNoAsignable` | El examen no existe, está inactivo, su asociación con el paso está inactiva o es de otro paso. |
| 400 `consulta.indicacionesValidas` | Las indicaciones superan 255 caracteres. |
| 400 `consulta.pasoPropio` | El paso no es de la consulta. |
| 409 `consulta.ordenPasoCompletado` | El paso ya está completado. |
| 409 `consulta.yaCerrada` | La consulta está cerrada. |

Las órdenes anteriores a la migración no tienen examen: en su JSON se omite `idExamen`, como los
demás campos nulos. La vista JSF de Consulta sigue creando órdenes solo con indicaciones.

**Despliegue.** La entidad `OrdenExamen` mapea `orden_examen.id_examen`: aplique
`docs/migrations/20261113_orden_examen_examen.sql` **antes** de desplegar este código; sin la
columna, toda lectura o escritura de órdenes falla. Los datos demo de exámenes están en
`docs/migrations/20261113_datos_desarrollo_examenes.sql` y los aplica
`scripts/preparar-bd-desarrollo.sh`.

El cierre conserva la referencia y las observaciones existentes cuando se omiten o se envían null. El cierre repetido no altera las fechas de una consulta ya cerrada. Los conflictos de estado, como editar una consulta cerrada, crear órdenes de pasos completados o cerrar con pendientes, devuelven 409. Las acciones de un paso también funcionan sobre su ruta anidada y comprueban que pertenezca al procedimiento indicado.

Un recorrido REST del flujo:

```text
PUT /api/atencion/rol-activo
{"idPersonaRol":"UUID_DE_ASIGNACION_MEDICA"}

POST /api/personas-roles/UUID_DE_ASIGNACION_PACIENTE/consultas
{"referenciaExterna":"REF-001","observaciones":"Consulta inicial"}

POST /api/consultas/UUID_DE_CONSULTA/procedimientos
{"idProcedimiento":"UUID_DEL_PROCEDIMIENTO","observaciones":"Notas"}

GET /api/consultas-procedimientos-pasos/UUID_DEL_PASO/examenes-asignables

POST /api/consultas-procedimientos-pasos/UUID_DEL_PASO/ordenes-examen
{"idExamen":"UUID_DEL_EXAMEN","indicaciones":"En ayunas"}

PUT /api/consultas-procedimientos-pasos/UUID_DEL_PASO/finalizacion
{"valor":"125/80"}

POST /api/ordenes-examen/UUID_DE_ORDEN/resultados
{"resultado":"Normal","interpretacion":"Sin hallazgos"}

PUT /api/consultas/UUID_DE_CONSULTA/cierre
{"referenciaExterna":"REF-001","observaciones":"Atención finalizada"}
```

Las órdenes y los exámenes se crean mientras el paso esté pendiente; una vez completado se avanza al siguiente. Los resultados pueden registrarse posteriormente. El catálogo y la disponibilidad de personal deben configurarse antes de iniciar procedimientos.

## Contrato HTTP

| Código | Uso |
| --- | --- |
| 200 | Lectura o actualización correcta. Un listado vacío devuelve `[]`. |
| 201 | Creación correcta: devuelve el DTO y `Location` con su URL. |
| 204 | Eliminación correcta, sin cuerpo. |
| 400 | JSON, UUID, campos obligatorios, longitudes, fechas o identificadores inconsistentes. |
| 401 | Autenticación requerida, cuando se configura en el contenedor. Conserva `WWW-Authenticate`. |
| 403 | Acceso denegado por el contenedor o una excepción de permisos. |
| 404 | Recurso, ruta o registro relacionado inexistente. |
| 405 | Método no permitido. Conserva `Allow`. |
| 406 | El cliente solicita un formato distinto de JSON. |
| 409 | Duplicados, concurrencia o restricciones de relaciones/negocio. |
| 415 | Cuerpo enviado con un tipo distinto de `application/json`. |
| 500 | Fallo interno. Los detalles técnicos se registran en el servidor. |

Otros estados HTTP se conservan en las excepciones del runtime, junto con cabeceras como `Retry-After`. Los errores usan el mismo contrato JSON:

```json
{
  "status": 404,
  "code": "recurso.noExiste",
  "message": "El recurso no existe.",
  "path": "/galenosv/api/clinicas/6a4ed47b-8d96-4d83-bbe3-653026ae5120",
  "timestamp": "2026-10-08T17:00:00Z",
  "details": []
}
```

Los errores de validación incluyen `details` con `field` y `message`, sin copiar el valor rechazado. Un fallo de validación de una respuesta o del motor de validación devuelve 500.

## Cuerpos y ejemplos

Las respuestas correctas contienen directamente el DTO o un arreglo de DTOs. Cada DTO conserva los nombres de los campos de su entidad (`idClinica`, `nombre`, etc.). Las relaciones se envían como UUID, por ejemplo `"idClinica":"6a4ed47b-8d96-4d83-bbe3-653026ae5120"`.

En `POST`, omitir el identificador propio: el servidor genera un UUID. En `PUT`, el identificador del cuerpo puede omitirse; si se incluye debe coincidir con la URL. `PUT` reemplaza los campos editables; enviar el DTO completo. Los campos opcionales editables omitidos se ponen a `null`. Los campos controlados por las nuevas operaciones del flujo clínico se conservan. Las relaciones obligatorias se indican en OpenAPI; el padre de una ruta anidada se infiere de la URL. `idProcedimientoPasoReferencia` identifica el destino de una secuencia; un paso inicial no requiere una secuencia entrante.

Las fechas `OffsetDateTime` usan ISO 8601 con zona (`2026-10-08T10:00:00-06:00`). Los campos basados en `Date`, incluida `fechaNacimiento`, requieren milisegundos y zona (`2000-01-01T06:00:00.000Z`). Las entidades conservan sus valores predeterminados de creación definidos en `@PrePersist`.

Crear una clínica:

```bash
curl -i -X POST http://localhost:9080/galenosv/api/clinicas \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Clínica Central","activo":true,"tipo":"General","comentarios":"Atención general"}'
```

Listar las clínicas:

```bash
curl -i -H 'Accept: application/json' http://localhost:9080/galenosv/api/clinicas
```

Consultar, actualizar y eliminar usando el UUID devuelto:

```bash
curl -i http://localhost:9080/galenosv/api/clinicas/UUID

curl -i -X PUT http://localhost:9080/galenosv/api/clinicas/UUID \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Clínica Central","activo":false,"tipo":"General","comentarios":"Horario actualizado"}'

curl -i -X DELETE http://localhost:9080/galenosv/api/clinicas/UUID
```

Crear una relación persona/rol/clínica:

```json
{
  "idPersona": "UUID_DE_PERSONA",
  "idRol": "UUID_DE_ROL",
  "idClinica": "UUID_DE_CLINICA"
}
```

Enviar ese objeto a `POST /api/personas-roles`, sustituyendo los valores por UUID existentes.

## OpenAPI y extensión

El contrato importable en Postman/Swagger está en `src/main/webapp/openapi.json` y se publica como `/galenosv/openapi.json`. Describe las 20 entidades, sus campos, filtros, colecciones anidadas, cuerpos y respuestas.

Para ampliar los filtros: declarar en `filtrosPermitidos()` el nombre público del parámetro y su ruta de atributos JPA hasta el UUID. Para publicar una colección: declararla en `coleccionesRelacionadas()` con el recurso hijo, el filtro que vincula el padre y las escrituras permitidas; el constructor de dos argumentos habilita solo lectura. `AbstractResource` resuelve el subrecurso, fija el padre antes de validar el DTO y comprueba la pertenencia mediante `AbstractService.perteneceAFiltros` y `AbstractRepository.matchesFilters`. El repositorio usa parámetros de consulta, comparte los JOIN de relaciones y elimina duplicados con `DISTINCT`. Cada recurso puede restringir `metodosEscritura()` y especializar `crearEntidad`, `actualizarEntidad` o `eliminarEntidad` para usar su servicio de flujo. Actualizar OpenAPI y las pruebas del contrato.

Para añadir una entidad: crear su DTO con validaciones, extender `AbstractResource<Entidad, EntidadDto>`, declarar `@Path`, `@Consumes` y `@Produces`, inyectar los servicios y completar `nuevaEntidad`, `toDto` y `aplicar`. El descubrimiento de `AppConfig` incluye el recurso automáticamente; actualizar también OpenAPI y las pruebas.

Los recursos delegan en los servicios existentes. El CRUD existente se conserva; las nuevas escrituras clínicas usan `AtencionService` para respetar reglas de clínica, personal, pasos, cierre e historial. Crear una persona dentro de una clínica usa `RegistroPersonaService`; configurar pasos y asociar exámenes usa `ConfiguracionProcedimientoService`. Estos servicios devuelven ahora las entidades que crean para producir el DTO y su `Location`. Los listados sin filtros devuelven todos los registros; los listados filtrados delegan en `AbstractService.listarPorFiltros` y `AbstractRepository.findByFilters`.

La base no agrega un mecanismo de inicio de sesión ni tokens. Los estados 401/403 traducen los rechazos de autenticación/autorización que se configuren en Jakarta EE o se lancen desde futuros recursos.

## Verificación

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn verify
```

`RestApiTest` ejecuta los recursos HTTP con Jersey en memoria, JSON-B y Bean Validation reales, usando servicios simulados. Verifica CRUD administrativo, filtros simples y combinados, las 26 colecciones anidadas, escrituras contextuales, pertenencia de hijos, PATCH, HEAD, OPTIONS, acciones clínicas, restricciones de las rutas del flujo, reglas de formato del catálogo, errores y cabeceras. `AbstractRepositoryFilterTest` comprueba JOIN compartidos, parámetros, eliminación de duplicados y pertenencia por registro y padre. Las pruebas de servicios conservan las reglas reales del flujo. Estas pruebas no conectan a PostgreSQL. Jersey solo pertenece al alcance `test`; el WAR usa el runtime Jakarta EE de Liberty.

Referencias: [Jakarta REST 4.0](https://jakarta.ee/specifications/restful-ws/4.0/jakarta-restful-ws-spec-4.0.html) y [Jersey Test Framework](https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest4x/test-framework.html).
