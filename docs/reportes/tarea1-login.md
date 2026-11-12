# Reporte de la tarea 1: login por documento (HU2)

Fecha: 2026-11-12. Rama: `backend-rubrica`. Base: `d3b9c71`. Resultado: 6 commits,
`mvn clean test` con **602 pruebas, 0 fallos, 0 errores, 0 omitidas** (línea base: 579).

## Decisiones aprobadas

1. Todo rechazo del login devuelve **401 `auth.credenciales`** con el mismo mensaje:
   "Documento no registrado o sin acceso autorizado."
2. `GET /api/publico/tipos-documento` devuelve `idTipoDocumento`, `nombre`,
   `indicaciones` y `expresionRegular`.
3. `usuario.password_hash` se conserva y se llena con el hash de una clave aleatoria.

## Commits

| Commit | Contenido | `mvn test` antes del commit |
| --- | --- | --- |
| `29ce21a` | Migración `20261112_documento_tipo_valor_unico.sql` y su prueba SQL | 579 / 0 fallos |
| `eb51dd1` | `UsuarioRepository.buscarPorDocumento`, asignaciones sin PACIENTE, `AutenticacionService.iniciar(UUID, String)` | 589 / 0 fallos |
| `d204ac3` | `AuthResource.Credenciales` → `idTipoDocumento` + `numeroDocumento`; se elimina el login por contraseña | 590 / 0 fallos |
| `f31857a` | `PublicoResource` (`GET /api/publico/tipos-documento`) y exclusiones en ambos filtros | 602 / 0 fallos |
| `629d35e` | Tipo DUI y documentos de desarrollo; el script ya no pide contraseña | 602 / 0 fallos |
| `4161c03` | `docs/api-rest.md`, `docs/autenticacion.md`, `openapi.json` | 602 / 0 fallos |

Cada commit incluyó solo los archivos de su paso (se mostró `git status` antes de
cada uno). No se tocaron `nbactions.xml` (borrado, sin staging) ni `generar_tests.sh`
e `inyectar_*.py` (sin seguimiento). No se hizo push.

## Cumplimiento de los requisitos

| Requisito | Estado | Dónde |
| --- | --- | --- |
| `POST /api/auth/login` con `{idTipoDocumento, numeroDocumento}` sin contraseña | Hecho | `AuthResource.java` |
| Documento por tipo y valor → Persona → Usuario activo | Hecho (una consulta con `EXISTS`; además exige tipo activo) | `UsuarioRepository.buscarPorDocumento` |
| Solo ADMINISTRACION o DOCTOR; rol inicial = la asignación de empleado más antigua, nunca PACIENTE | Hecho (en la consulta y otra vez en el servicio) | `UsuarioRepository.asignaciones`, `AutenticacionService.empleados` |
| Se conservan IntentosAcceso, la rotación de sesión, el CSRF y el 204 | Hecho; verificado en `AuthResourceTest` | `AuthResource.login` |
| `GET /api/publico/tipos-documento` sin sesión, solo activos | Hecho | `PublicoResource`, `TipoDocumentoRepository.listarActivos` |
| Exclusión de `/api/publico/` en AutenticacionFilter | Hecho; usa `servletPath + pathInfo` normalizados | `AutenticacionFilter.rutaPublica` |
| Exclusión en PermisosApiFilter | Hecho; raíz `publico` | `PermisosApiFilter.filter` |
| Migración UNIQUE (id_tipo_documento, valor) con detección previa de duplicados | Hecho; se puede ejecutar varias veces | `docs/migrations/20261112_documento_tipo_valor_unico.sql` |
| Datos de desarrollo: DUI con su expresión regular y documentos de admin.demo y doctor.demo | Hecho (paciente.demo también, para probar el rechazo) | `20261111_datos_desarrollo.sql` |
| Script de preparación actualizado | Hecho | `scripts/preparar-bd-desarrollo.sh` |
| Documentación | Hecho | `docs/api-rest.md`, `docs/autenticacion.md`, `openapi.json` |

## Qué se rompía y cómo se resolvió

- `AutenticacionService.iniciar(String, String)` y `UsuarioRepository.buscarAcceso`
  solo se usaban en `AuthResource` y en las pruebas. Se eliminaron en `d204ac3`. Para
  que `eb51dd1` compilara, el método nuevo convivió un commit como sobrecarga.
- `AuthResource.Credenciales` cambió de campos. Ninguna vista JSF ni bean usaba
  login, password ni `iniciar`, así que no hubo cambios en `paginas/` ni en `boundary/model`.
- `ContrasenaService` y `ContrasenaServiceTest` se conservan: el script los usa para
  llenar `password_hash`, que el esquema sigue exigiendo (`NOT NULL` + CHECK Argon2id).
- `AutenticacionService` ya no inyecta `ContrasenaService`. Una prueba lo verifica.

## Pruebas JUnit

| Clase | Casos relevantes |
| --- | --- |
| `AutenticacionServiceTest` (12) | Documento inexistente → 401 y no consulta asignaciones; documento de otro tipo → se busca con el tipo recibido → 401; usuario inactivo → 401; **PACIENTE → 401**; sin asignación → 401; con PACIENTE (más antigua), DOCTOR y ADMINISTRACION gana DOCTOR; tipo nulo o número vacío → 401 sin consultar; el servicio no depende de contraseñas; `contexto()` rechaza una asignación PACIENTE, una ajena y una cuenta inactiva. |
| `UsuarioRepositoryTest` (5) | Tipo y número recortado se pasan como parámetros (nada se concatena); sin coincidencia → `null`; entradas vacías no consultan; el JPQL de asignaciones contiene ADMINISTRACION y DOCTOR, no PACIENTE, y ordena por `fechaCreacion, idPersonaRol`; `buscarAcceso` ya no existe. |
| `AuthResourceTest` (8) | Login correcto: 204, `registrar(IP)`, invalida la sesión anterior, `changeSessionId`, CSRF; un rechazo cuenta el intento sin crear sesión; un 429 no llega al servicio; `Credenciales` no tiene `password` y Bean Validation rechaza tipo nulo, número vacío, más de 50 caracteres y CR/LF. |
| `AutenticacionFilterTest` (21) | **La ruta pública no exige sesión** ni la crea y conserva `no-store`/`nosniff`; `/api/publico/../personas` → 401; `/publicoX/…`, `/publico` y `/personas/publico/x` → 401. |
| `PermisosApiHttpTest` (14) | `publico/tipos-documento` → 200 sin llamar a `auth.contexto`; `tipos-documento` sigue exigiendo sesión; `publico/../consultas/{id}`, `publico/%2e%2e/…` y `publico/./../…` no devuelven 200. |
| `PublicoResourceTest` (3) | Solo activos y exactamente los cuatro campos; opcionales vacíos → `null`; el recurso no inyecta componentes de sesión. |
| `TipoDocumentoRepositoryTest` (1), `TipoDocumentoServiceTest` (+1) | `activo=true ORDER BY t.nombre`; el servicio delega en el repositorio. |

Todas se escribieron antes que el código y se vio cada una fallar por la razón esperada
(método o clase inexistente, o comportamiento ausente) antes de implementar.

## Verificación de SQL

No se usó docker. Se levantó un PostgreSQL local temporal (`initdb` en el directorio
temporal de la sesión, puerto 54329 en 127.0.0.1), que se detuvo y se borró al terminar. Resultados:

- Base completa + migración: aplica correctamente; una segunda ejecución también.
- `docs/migrations/tests/documento_tipo_valor_unico_test.sql`: pasa.
- Con dos documentos de igual tipo y valor, la migración aborta con
  "Hay 1 grupos… id_documento: …", sin mostrar los valores.
- Datos de desarrollo ejecutados dos veces: 3 documentos, 1 tipo; admin.demo
  (ADMINISTRACION), doctor.demo (DOCTOR), paciente.demo sin rol de empleado.
- Con un tipo `' Dui '` ya existente, se reutiliza y no se duplica.
- Si `00000000-1` ya pertenece a otra persona, el script aborta y no escribe nada.
- La cadena `head -c 48 /dev/urandom | base64 | ContrasenaService --stdin` produce un
  hash que pasa el CHECK de la base.

`preparar-bd-desarrollo.sh` completo **no se ejecutó**, porque usa docker. Se comprobó su sintaxis (`bash -n`)
y por separado cada uno de sus pasos.

## Hallazgo de seguridad corregido

Al agregar la exclusión de `publico` en `PermisosApiFilter`, una prueba mostró que
`GET publico/../consultas/{id}` llegaba al recurso **sin comprobar permisos** en la
pila Jersey de las pruebas. Con una sesión válida, un DOCTOR habría podido leer
consultas de otra clínica. Se corrigió en `f31857a`: cualquier segmento `.` o `..` devuelve
400 `ruta.invalida`. En `AutenticacionFilter` la exclusión usa el `pathInfo` normalizado
por el contenedor, así que sin sesión esa ruta ya devolvía 401.

## Limitaciones y pendientes

- **Sin pruebas JPA reales.** El proyecto no tiene un runtime JPA en las pruebas. El JPQL de
  `buscarPorDocumento`, `asignaciones` y `listarActivos` se verifica por su texto y sus
  parámetros, no contra EclipseLink. Hay que probarlo en Liberty con la base de desarrollo:
  admin.demo y doctor.demo → 204; paciente.demo, número inexistente y tipo equivocado → 401.
- El 400 por un `idTipoDocumento` que no es UUID depende de `JsonBodyReaderInterceptor`
  (ya existente). No hay una prueba HTTP específica para el login.
- Sin contraseña, el límite de `IntentosAcceso` (10 intentos por IP cada 5 minutos, en memoria)
  es la única barrera contra la enumeración de DUIs. Detrás de un proxy, todas las solicitudes
  comparten la IP.
- La rama PACIENTE de `PermisosApiFilter` quedó sin uso, porque una sesión ya no puede tener
  ese rol. No se eliminó por estar fuera del alcance.
- La sección "Alcance de esta etapa" de `docs/autenticacion.md` es anterior a esta tarea y no
  se revisó.
- `docs/revision-backend.md` no existe. Las líneas de los filtros se tomaron del código.
- Este reporte no está en ningún commit.
