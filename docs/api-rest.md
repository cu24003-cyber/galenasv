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

Cada ruta de la tabla admite `GET` (listar) y `POST` (crear). La variante `/{id}` admite `GET`, `PUT` y `DELETE`.

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

En `POST`, omitir el identificador propio: el servidor genera un UUID. En `PUT`, el identificador del cuerpo puede omitirse; si se incluye debe coincidir con la URL. `PUT` reemplaza todos los campos editables; enviar el DTO completo. Los campos opcionales omitidos se ponen a `null`. Las relaciones obligatorias se indican en el contrato OpenAPI. `idProcedimientoPasoReferencia` permite `null` para una secuencia inicial.

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

El contrato importable en Postman/Swagger está en `src/main/webapp/openapi.json` y se publica como `/galenosv/openapi.json`. Describe las 20 entidades, sus campos, rutas, cuerpos y respuestas.

Para añadir una entidad: crear su DTO con validaciones, extender `AbstractResource<Entidad, EntidadDto>`, declarar `@Path`, `@Consumes` y `@Produces`, inyectar los servicios y completar `nuevaEntidad`, `toDto` y `aplicar`. El descubrimiento de `AppConfig` incluye el recurso automáticamente; actualizar también OpenAPI y las pruebas.

Los endpoints son CRUD de entidades y delegan en los métodos CRUD de los servicios existentes. Los flujos compuestos de atención (`AtencionService`, registro con rol, examen con clasificaciones, etc.) mantienen sus propios métodos y requieren recursos específicos cuando se quieran publicar esas operaciones. Los listados actuales devuelven todos los registros.

La base no agrega un mecanismo de inicio de sesión ni tokens. Los estados 401/403 traducen los rechazos de autenticación/autorización que se configuren en Jakarta EE o se lancen desde futuros recursos.

## Verificación

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn verify
```

`RestApiTest` ejecuta el CRUD HTTP de las 20 entidades con Jersey en memoria, JSON-B y Bean Validation reales, usando servicios simulados. Verifica también errores, relaciones, formatos, UUID, fechas, cabeceras y ausencia de detalles internos. Estas pruebas no conectan a PostgreSQL. Jersey solo pertenece al alcance `test`; el WAR usa el runtime Jakarta EE de Liberty.

Referencias: [Jakarta REST 4.0](https://jakarta.ee/specifications/restful-ws/4.0/jakarta-restful-ws-spec-4.0.html) y [Jersey Test Framework](https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest4x/test-framework.html).
