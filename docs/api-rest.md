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
| `/api/personas-roles/{id}/pasos-consulta` | `ConsultaProcedimientoPasoDto` |
| `/api/procedimientos/{id}/pasos` | `ProcedimientoPasoDto` |
| `/api/procedimientos/{id}/consultas-procedimientos` | `ConsultaProcedimientoDto` |
| `/api/procedimientos-pasos/{id}/examenes-configurados` | `ProcedimientoPasoExamenDto` |
| `/api/procedimientos-pasos/{id}/secuencias` | `ProcedimientoPasoSecuenciaDto` |
| `/api/procedimientos-pasos/{id}/pasos-consulta` | `ConsultaProcedimientoPasoDto` |
| `/api/roles/{id}/personas` | `PersonaDto` |
| `/api/roles/{id}/personas-roles` | `PersonaRolDto` |
| `/api/roles/{id}/procedimientos-pasos` | `ProcedimientoPasoDto` |
| `/api/tipos-documento/{id}/documentos` | `DocumentoDto` |
| `/api/tipos-examen/{id}/examenes` | `ExamenDto` |
| `/api/tipos-examen/{id}/clasificaciones` | `ExamenTipoExamenDto` |
| `/api/tipos-medio-contacto/{id}/medios-contacto` | `MedioContactoDto` |

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
| POST /api/consultas-procedimientos-pasos/{id}/ordenes-examen | Crea una orden de un paso pendiente de la consulta. |
| POST /api/ordenes-examen/{id}/resultados | Registra un resultado de una orden de la consulta, incluso después del cierre. |

Las nuevas rutas del flujo no habilitan DELETE del historial ni reemplazo genérico de procedimientos ejecutados, pasos, órdenes o resultados. En estas rutas las fechas y los estados se generan en el servidor: no enviar fechas en POST; el paciente y las fechas se conservan al editar las notas de una consulta. El CRUD directo anterior mantiene su contrato. Para las transiciones se usan estas operaciones:

| Método y ruta | Cuerpo | Comportamiento |
| --- | --- | --- |
| `PUT /api/atencion/rol-activo` | `idPersonaRol` | Selecciona rol y clínica; conserva la restricción de cambio de rol durante una atención. |
| `PUT /api/consultas/{id}/sesion` | Sin cuerpo | Retoma una consulta abierta de la clínica activa. |
| `PUT /api/consultas/{id}/cierre` | `referenciaExterna`, `observaciones` opcionales | Cierra cuando los pasos están completos y cada procedimiento llega a un paso final; libera la consulta en sesión. |
| `PUT /api/consultas-procedimientos-pasos/{id}/responsable` | `idPersonaRol` | Cambia el responsable de un paso pendiente, respetando rol activo y clínica. |
| `PUT /api/consultas-procedimientos-pasos/{id}/finalizacion` | `valor` opcional | Completa el paso y genera sus sucesores cuando se cumplen los predecesores. |
| `POST /api/consultas-procedimientos-pasos/{id}/examenes` | `nombre`, `idTipoExamen`, `observaciones` opcionales | Crea examen, clasificación y asociación al paso en una transacción. |

El cierre conserva la referencia y las observaciones existentes cuando se omiten o se envían null. El cierre repetido no altera las fechas de una consulta ya cerrada. Los conflictos de estado, como editar una consulta cerrada, crear órdenes de pasos completados o cerrar con pendientes, devuelven 409. Las acciones de un paso también funcionan sobre su ruta anidada y comprueban que pertenezca al procedimiento indicado.

Un recorrido REST del flujo:

```text
PUT /api/atencion/rol-activo
{"idPersonaRol":"UUID_DE_ASIGNACION_MEDICA"}

POST /api/personas-roles/UUID_DE_ASIGNACION_PACIENTE/consultas
{"referenciaExterna":"REF-001","observaciones":"Consulta inicial"}

POST /api/consultas/UUID_DE_CONSULTA/procedimientos
{"idProcedimiento":"UUID_DEL_PROCEDIMIENTO","observaciones":"Notas"}

POST /api/consultas-procedimientos-pasos/UUID_DEL_PASO/ordenes-examen
{"indicaciones":"Hemograma"}

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
