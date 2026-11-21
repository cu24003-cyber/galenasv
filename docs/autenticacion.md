# Acceso de ChivoMed

Requiere `docs/migrations/20261111_base_completa.sql`, `20261112_documento_tipo_valor_unico.sql`
y `20261115_login_por_documento.sql`. El login busca un documento existente vinculado a una
persona; el tipo del documento debe estar activo. Ya no existe la tabla `usuario`.

**HU2:** el login se hace con tipo y número de documento, sin contraseña ni cuenta adicional.
Cualquier persona con un documento existente de tipo activo puede iniciar sesión. El acceso
a las páginas y operaciones protegidas sigue dependiendo de su rol; los endpoints validan la
asignación, su estado y su clínica en cada solicitud.

## Ejecución local

```bash
mvn verify
mvn liberty:install-feature
bash scripts/liberty.sh
```

La configuración local de Liberty debe apuntar a PostgreSQL. `server.xml` referencia
variables de entorno; las credenciales están en `.env`, excluido de Git.
El script carga ese archivo; para configurar los valores, vea `docs/LEVANTAR.md` §3. La base de Julio en Docker se llama
`galenasv`; cada compañero debe utilizar los datos de su entorno.

En ChivoMed, ejecutar `npm start` o Run de WebStorm. BrowserSync sirve el frontend
en `http://127.0.0.1:3000` y envía `/galenosv/api` al backend en el puerto 9080.
El navegador conserva la cookie; no es necesario habilitar CORS para este recorrido.

Las escrituras de autenticación y cambio de rol comprueban `Origin` o `Referer`.
Se acepta el origen propio de GalenoSV y, para desarrollo, `http://127.0.0.1:3000`
y `http://localhost:3000`. Para otros orígenes de confianza, establecer la variable
`CHIVOMED_ORIGINS` con una lista separada por comas antes de iniciar Liberty.
No usar comodines. La configuración de ejemplo usa cookie `HttpOnly`, `SameSite=Lax`
y ruta del contexto. En producción se requiere HTTPS y `cookieSecure=true`.

## Contrato HTTP

| Operación | Entrada | Resultado |
| --- | --- | --- |
| `GET /api/publico/tipos-documento` | Nada (sin cookie ni CSRF) | Tipos activos: `idTipoDocumento`, `nombre`, `indicaciones`, `expresionRegular`. |
| `POST /api/auth/login` | JSON con `idTipoDocumento` (UUID) y `numeroDocumento` | 204 y cookie de sesión nueva. |
| `GET /api/auth/me` | Cookie de sesión | Identidad de la persona, rol activo o `null`, y token CSRF. |
| `POST /api/auth/logout` | Cookie y cabecera `X-CSRF-Token` | 204; invalida toda la sesión. |
| `GET /api/atencion/asignaciones` | Cookie de sesión | Asignaciones de empleado activas de la persona en todas sus clínicas; `activa` marca el rol activo. Sin sesión: 401 `auth.sesion`. |
| `GET /api/atencion/dashboard` | Cookie de sesión y rol activo (cualquier rol de empleado) | Consultas de la clínica del rol activo (HU3). El servicio exige sesión y revalida la asignación: 401 `auth.sesion`, 403 `auth.rolActivo` o `auth.asignacion`. |
| `GET /api/atencion/dashboard/pasos/{id}/valor` | Cookie de sesión y rol activo | Valor de un paso de la misma clínica; si no, 404 `consulta.pasoNoExiste`. |
| `PUT /api/atencion/rol-activo` | Cookie, token CSRF y `idPersonaRol` | Solo admite asignaciones propias y renueva el identificador de sesión. Con una consulta en curso y otra asignación: 409 `rolCambio.cerrarAntesCambiar`. |

El documento se busca por el par exacto (tipo, valor); el número se recorta pero
no se normaliza de otra forma (el DUI se escribe con guion: `01000001-1`). El tipo
debe estar activo. `numeroDocumento` admite hasta 50 caracteres sin saltos de línea;
un UUID inválido o un cuerpo mal formado devuelven 400.

Los rechazos de login por documento inexistente, tipo inactivo o falta de persona devuelven
401 `auth.credenciales` con el mismo mensaje neutro. La falta de rol no impide autenticarse,
pero sí impide entrar a páginas y operaciones protegidas. La respuesta y el estado usan
`Cache-Control: no-store`.

Rol inicial: la asignación activa de empleado más antigua; si no existe, se toma la asignación
activa más antigua, incluso PACIENTE. Si no hay asignaciones activas, `rolActivo` es `null`.
El frontend conserva sus rutas de empleado protegidas; el backend aplica los permisos por rol.

`/auth/me` devuelve exactamente el contrato utilizado por el frontend:

```text
usuario: { idUsuario, idPersona, nombres, apellidos }
rolActivo: { idPersonaRol, idRol, codigo, nombreRol, idClinica, nombreClinica } | null
csrfToken: token aleatorio asociado a la sesión
```

Por compatibilidad, `usuario.idUsuario` se devuelve como alias de `idPersona`; no corresponde a
una fila de cuenta.

`idRol` es el UUID del rol: se compara con `rolPaso.idRol` del dashboard para saber qué pasos
atiende el empleado. `codigo` es el código estable del rol (`ADMINISTRACION`, `DOCTOR`, `ENFERMERA`…) y es
`null` en un rol creado desde la app sin código; `nombreRol` es el nombre visible (por
ejemplo "Atención al público" para `ADMINISTRACION`). El frontend debe mostrar
`nombreRol` y no deducir permisos del código, salvo `ADMINISTRACION` (recepción y
catálogos).

No devuelve hashes ni contraseñas. Cada consulta de sesión comprueba que la persona exista y
conserve un documento de tipo activo; también revalida la asignación de rol activa. Si la
persona desaparece, todos sus tipos de documento se desactivan o se revoca el rol, se invalida
la sesión. Se conservan los límites clínicos y permisos por rol.

La sesión expira tras 30 minutos de inactividad o un máximo de ocho horas.
Como no hay contraseña, este límite es la principal protección contra
la enumeración de números de documento. Son diez solicitudes de login por dirección
en cinco minutos; devuelve 429 y `Retry-After: 300`. Se mantiene en memoria por instancia y
no confía en cabeceras de IP enviadas por el cliente. Con BrowserSync, las conexiones
al backend comparten la dirección del proxy.

## Rutas públicas

`AutenticacionFilter` deja pasar `/api/publico/*` sin cookie. Para decidirlo usa
`getServletPath()` y `getPathInfo()`, que el contenedor decodifica y normaliza, y no
la URI cruda: `/api/publico/../personas` exige sesión. `PermisosApiFilter` excluye la
raíz `publico` y rechaza con 400 `ruta.invalida` cualquier segmento `.` o `..`.
Las rutas públicas nunca devuelven notas, valores clínicos ni resultados.

La búsqueda de citas (HU1: `POST /api/publico/clinicas`, `/citas` y `/citas/{id}/ticket`)
lleva el documento en el cuerpo, comparte un límite de 60 solicitudes por IP cada 5 minutos,
separado del login (10), y responde igual para un documento inexistente que para uno sin
datos. Detalle en `docs/api-rest.md`. Los límites viven en memoria por instancia y detrás de
un proxy todas las solicitudes comparten la IP.

## Alta de personas

Para que una persona pueda autenticarse, debe existir, tener un documento asociado y que el
tipo de ese documento esté activo. Los roles y las clínicas se mantienen en `persona_rol` y
determinan las páginas y operaciones disponibles; no se crea una cuenta adicional ni se asigna
una contraseña. No hay endpoint público para modificar documentos o asignaciones.

### Entorno de desarrollo

Después de `mvn -DskipTests package`, `bash scripts/preparar-bd-desarrollo.sh` aplica la base
completa, las migraciones y los datos demo del guion del Primer Parcial
(`docs/migrations/20261113_datos_demo_rubrica.sql` y `…_consultas.sql`; detalle en
`docs/datos-demo-rubrica.md` y `docs/LEVANTAR.md` §10). Tipos de documento activos: DUI
(`^[0-9]{8}-[0-9]$`) y Pasaporte (`^[A-Z][0-9]{8}$`). Todos los números son inventados:

| Persona | Documento | Resultado del login |
| --- | --- | --- |
| Carlos Gómez | DUI `01000001-1` o Pasaporte `A10000001` | 204; rol inicial Doctor en Clínica Santa Ana (también Doctor en San Salvador) |
| Lucía Pérez | DUI `01000002-2` | 204; Enfermera en Clínica Santa Ana |
| Marta López | DUI `01000003-3` | 204; Atención al público (`ADMINISTRACION`) en Santa Ana y en San Salvador |
| Rosa Méndez | DUI `01000006-6` | 204; Enfermera en Clínica San Salvador |
| Jorge Ruiz | DUI `01000007-7` | 204; Doctor en Clínica San Salvador |
| Ana Martínez, Julia Flores, Juan Vásquez | DUI `01000004-4`, `01000008-8`, `01000009-9` | 204; el rol PACIENTE no abre páginas de empleado |
| Pedro Hernández | DUI `01000005-5` | 204; sin rol no puede abrir páginas protegidas |
| Nadie | `12345678-9` | 401 con el mismo mensaje neutro |

Los roles Enfermera (`ENFERMERA`, activo) y Farmacéutico (`FARMACEUTICO`, inactivo) se crean
con la demo; `ADMINISTRACION` pasa a llamarse "Atención al público" y conserva su código.

Si se vuelve a ejecutar, deja los catálogos y personas demo como en el archivo, inserta las
consultas que falten y ancla de nuevo sus fechas a hoy sin cambiar estados. Para devolver las 11
consultas a su estado inicial después de usarlas:

```bash
bash scripts/reiniciar-demo.sh [contenedor] [base] [usuario_postgres]
# equivale a: psql -v reiniciar=1 -f docs/migrations/20261113_datos_demo_rubrica_consultas.sql
```

El reinicio pide confirmación y solo afecta las 11 consultas con UUID `de120010-…`: borra los
resultados, las órdenes y los pasos o procedimientos que se les hayan agregado desde la app, y
restaura fechas, estados, valores, notas y las dos órdenes demo. No toca ningún otro dato.

La demo anterior (admin.demo, doctor.demo y paciente.demo, DUI `00000000-1/2/3`) se borra con
`docs/migrations/20261113_limpieza_demo_anterior.sql`, después de un respaldo
(`docs/LEVANTAR.md` §5.1).

## Alcance de esta etapa

El login identifica a la persona por un documento de tipo activo. Las páginas de personal
siguen protegidas por el rol y la asignación activa; el rol PACIENTE no habilita esas páginas.
Las rutas públicas de citas conservan su propio flujo. Los cambios de esta tarea no se han
aplicado a ninguna base de datos.

## Resumen de archivos

Los archivos Java de implementación están bajo
`src/main/java/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv/`.

| Estado | Archivo | Cambio |
| --- | --- | --- |
| Modificado | `entity/repository/DocumentoRepository.java` | Resuelve la persona por documento de tipo activo. |
| Modificado | `entity/Rol.java` | Mapea el código estable del rol. |
| Modificado | `control/service/AutenticacionService.java` | Login por documento y validación de persona y permisos por rol. |
| Nuevo | `boundary/resources/PublicoResource.java` | `GET /api/publico/tipos-documento` sin sesión. |
| Modificado | `boundary/resources/provider/PermisosApiFilter.java` | Excluye `publico` y rechaza segmentos `.` y `..`. |
| Nuevo | `docs/migrations/20261112_documento_tipo_valor_unico.sql` | UNIQUE (id_tipo_documento, valor) con detección previa de duplicados. |
| Nuevo | `control/service/AccesoSesion.java` | Identidad, token CSRF y duración máxima de la sesión. |
| Nuevo | `control/service/AccesoException.java` | Errores seguros de autenticación y autorización. |
| Nuevo | `control/service/IntentosAcceso.java` | Límite de solicitudes de login por dirección. |
| Modificado | `control/service/ContextoRolService.java` | Lista únicamente las asignaciones de la persona autenticada. |
| Nuevo | `boundary/resources/AuthResource.java` | Login, sesión actual y logout. |
| Nuevo | `boundary/web/AutenticacionFilter.java` | Cookie, origen, CSRF y cabeceras de autenticación. |
| Modificado | `boundary/resources/AtencionResource.java` | Revalida el contexto y rechaza cambios a roles ajenos. |
| Modificado | `boundary/resources/provider/ApiExceptionMapper.java` | Conserva códigos y mensajes seguros de acceso. |
| Modificado | `pom.xml` | Retira la dependencia Password4j, que ya no se usa. |
| Modificado | `src/main/resources/META-INF/persistence.xml` | Retira la entidad de cuentas del mapeo JPA. |
| Modificado | `src/main/webapp/WEB-INF/web.xml` | Cookie HttpOnly y seguimiento de sesión solo por cookie. |
| Modificado | `src/main/liberty/config/server.xml.example` (hoy `server.xml.ejemplo`) | Ejemplo de SameSite y ruta de cookie. |
| Modificado | `src/main/webapp/openapi.json` | Documenta el contrato de autenticación y CSRF. |
| Modificado | `src/test/.../boundary/resources/RestApiTest.java` | Adapta el contexto autenticado y comprueba que el código del rol no sea editable. |
| Nuevo | `src/test/.../boundary/resources/AuthResourceTest.java` | Contrato de sesión, renovación e invalidación. |
| Nuevo | `src/test/.../boundary/web/AutenticacionFilterTest.java` | Origen, cookies, expiración y CSRF. |
| Nuevo | `src/test/.../control/service/AccesoSesionTest.java` | Tokens independientes y vencimiento absoluto. |
| Nuevo | `docs/migrations/20261115_login_por_documento.sql` | Elimina la tabla `usuario` del esquema vigente. |
| Modificado | `src/test/.../control/service/AutenticacionServiceTest.java` | Documento y tipo activos, persona existente, PACIENTE, ausencia de rol y roles ajenos. |
| Nuevo | `src/test/.../control/service/ContextoRolServiceTest.java` | Listado propio y rechazo de asignaciones ajenas. |
| Nuevo | `src/test/.../control/service/IntentosAccesoTest.java` | Límite y renovación de la ventana de intentos. |
| Eliminado | `entity/Usuario.java`, `entity/repository/UsuarioRepository.java` | Ya no forman parte del modelo de autenticación. |
| Eliminado | `control/service/ContrasenaService.java` | No se almacenan ni validan contraseñas. |
| Nuevo | `src/test/.../boundary/resources/PublicoResourceTest.java` | Solo tipos activos y solo los cuatro campos públicos. |
| Nuevo | `docs/autenticacion.md` | Instrucciones, contrato y alcance de esta etapa. |

La configuración `src/main/liberty/config/server.xml` y las credenciales
`.env` y `.env.login-local` son exclusivas del entorno local y están excluidas de Git.
La cuenta de Julio y la clínica de desarrollo están únicamente en su PostgreSQL;
no se añaden como datos predeterminados para los demás entornos.

Para detener el servidor local: `mvn liberty:stop`.
