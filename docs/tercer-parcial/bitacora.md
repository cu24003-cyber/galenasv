# Bitácora del Tercer Parcial PPI115 2026 GT02 — Backend

Registro de qué se cambió en el backend para el tercer parcial, por qué y con qué evidencia.
Rama: `backend-rubrica`. Una entrada por cambio; los commits nuevos se agregan al final de la tabla.

## Contexto que motiva los cambios

- El profesor ejecuta la imagen con `docker run` en la red `ppi115net`, monta `galeno.war` en
  `/home/usuario/wlp/usr/servers/galeno/dropins/` y conecta con 5 variables: `PG_USER`, `PG_PASS`,
  `PG_DB`, `PG_HOST`, `PG_PORT`.
- La base de calificación es el **esquema original del ingeniero + su parche**, sin datos
  (`docker/prueba-bd/`). No tiene `rol.codigo` ni `orden_examen.id_examen`, y aparte de las PK
  casi no tiene restricciones `UNIQUE`.
- El frontend se conecta por `http://galeno.backend:9080/galeno`, así que el context root debe ser `/galeno`.
- Todas las imágenes deben usar `debian:stable`, y las dependencias del WAR deben ser `provided`.

## Cambios

| Fecha | Commit | Qué se cambió | Por qué | Criterio de la rúbrica |
| --- | --- | --- | --- | --- |
| 2026-11-20 | `4524e28` | Liberty lee la conexión de `PG_USER`, `PG_PASS`, `PG_DB`, `PG_HOST`, `PG_PORT` (`server.xml.ejemplo`, `.env.example`, `scripts/liberty.sh`) | El pool de conexiones debe configurarse solo con esas variables | 2. Backend: "gestiona correctamente las 5 variables" |
| 2026-11-21 | `4b6c21e` | `Rol.codigo` pasa a calcularse a partir del nombre (`@Transient`). Se rechazan documentos ambiguos en el login y en la consulta pública | `rol.codigo` no existe en la base original; sin una restricción única, un documento podría estar en dos personas | 2. Backend: "despliega" sin errores de esquema |
| 2026-11-21 | `e412f9e` | El examen de cada orden se guarda dentro de `orden_examen.indicaciones` con formato versionado (`ContenidoOrden`); las órdenes antiguas de solo texto se siguen leyendo | `orden_examen.id_examen` no existe en la base original | 2. Backend (HU4 del frontend sigue funcionando) |
| 2026-11-22 | `6cb2cf7` | `Rol.normalizarNombre()`: quita tildes, pasa a minúsculas, hace trim y une espacios | "Atencion al publico" sin tildes no se reconocía como Administración | 2. Backend: permisos correctos con datos que escriba el profesor |
| 2026-11-22 | `6a048b4` | Se rechazan roles con nombre duplicado tras normalizar (409 `rol.nombreDuplicado`); el rol "Paciente" que se crea al arrancar respeta esa regla | Evitar dos roles "Paciente" cuando el profesor cree el suyo | 2. Backend |
| 2026-11-22 | `66a20d7` | 409 si un documento (tipo + valor) ya es de otra persona, o si se repite persona + rol + clínica | La base original no tiene esas restricciones `UNIQUE` | 2. Backend |
| 2026-11-22 | `8c835a3` | `finalName` = `galeno`; `openpdf`, `postgresql`, `primefaces` y `primefaces-themes` pasan a `provided`; las rutas se documentan como `/galeno` | El WAR debe llamarse `galeno.war` (context root `/galeno`) y no llevar librerías | 1. Entrega: ".war con dependencias provided" |
| 2026-11-22 | (este commit) | `docker/Dockerfile` (`debian:stable`, Java 21, Open Liberty kernel + `jakartaee-11.0`, driver y librerías `provided`), `docker/server.xml` (`host="*"`, `jdbc/postgres` con `PG_*`, sin LTPA), `docker/prueba-bd/` (original + parche) y `docker/README.md` | Construir la imagen exigida y probar con una base idéntica a la de calificación | 2. Backend: imagen `debian:stable`, monta el WAR, 5 variables, red `ppi115net` |

| 2026-11-22 | `9a4e7a3` | `docker/Dockerfile`: `ENV JAVA_HOME=/usr/lib/jvm/java-21` (enlace al JDK detectado, sin fijar la arquitectura) | Sin `JAVA_HOME`, `bin/server` descartaba el Java del PATH por no tener `include/` y no cargaba `java9.options`; la app no arrancaba (`WELD-001524`, `module java.base does not "opens java.lang"`) | 2. Backend: "despliega" |

## Evidencia de verificación

| Fecha | Qué se verificó | Resultado | Quién |
| --- | --- | --- | --- |
| 2026-11-22 | `mvn clean test` después de `6cb2cf7`–`8c835a3` | 832 pruebas, 0 fallos, 0 errores (antes: 815) | Junior (reportado) |
| 2026-11-22 | `unzip -l target/galeno.war \| grep WEB-INF/lib` | Sin salida (WAR sin librerías) | Junior (reportado) |
| 2026-11-22 | Las 20 entidades contra `01-original.sql` + `02-parche.sql` | Ninguna mapea columnas inexistentes; las longitudes 20 y 155 coinciden | Revisión estática |
| 2026-11-22 | Los `DEFAULT` del esquema (`fecha_creacion`, `activo`, etc.) | 12 entidades los asignan con `@PrePersist` | Revisión estática |
| 2026-11-22 | `mvn clean package` (`5ef197c`) | BUILD SUCCESS; 832 pruebas, 0 fallos, 0 errores; `unzip -l target/galeno.war \| grep WEB-INF/lib` sin salida | Claude Code |
| 2026-11-22 | `docker build -t local/galeno-backend:1.0 docker/` (builder clásico, sin buildx) | Éxito. **Open Liberty 26.0.0.11** (`OL_VERSION=latest`), Debian 13.7 (`debian:stable`), OpenJDK 21.0.13.1. `lib/global`: openpdf-2.4.0, primefaces-16.0.0, primefaces-themes-16.0.0; `shared/resources/postgresql`: postgresql-42.7.5 | Claude Code |
| 2026-11-22 | Base de prueba (`postgres:16`, red `ppi115net`, `db.penya.org`, `docker/prueba-bd`) | Éxito: `01-original.sql` y `02-parche.sql` sin errores; 20 tablas; `consulta_procedimiento_paso` con `id_procedimiento_paso` y `valor` | Claude Code |
| 2026-11-22 | Comando del profesor, 1.er intento (imagen de `5ef197c`) | **Error**: `CWWKZ0002E … WELD-001524: Unable to load proxy class for bean Built-in Bean [jakarta.transaction.UserTransaction]`, causado por `InaccessibleObjectException: … module java.base does not "opens java.lang" to unnamed module`. Corregido en `9a4e7a3` | Claude Code |
| 2026-11-22 | Comando del profesor, con `9a4e7a3` (host `-p 9081:9080` porque el 9080 lo ocupaba el Liberty local; el resto, idéntico) | Éxito: `CWWKZ0001I: Application galeno started in 41.942 seconds`, `CWWKT0016I … http://galeno.backend:9080/galeno/`, `CWWKF0011I`. Aviso: `CWWKS9660E` (el ORB pide user registry) y `CWWKZ0022W`, que retrasan el arranque ~30 s | Claude Code |
| 2026-11-22 | `curl -i http://localhost:9081/galeno/api/publico/tipos-documento` | `HTTP/1.1 200 OK`, cuerpo `[]` | Claude Code |
| 2026-11-22 | Pantalla JSF `/galeno/paginas/tipoDocumento.xhtml` (también `rol.xhtml`, `clinica.xhtml`) | **Error 500**: `jakarta.faces.FacesException: Undefined component type org.primefaces.component.CommandButton`. PrimeFaces 16 no declara componentes en su `faces-config.xml`: los registra con `@FacesComponent`, y Liberty no escanea anotaciones de la librería global. Probado sin éxito: `org.apache.myfaces.annotation.SCAN_PACKAGES=org.primefaces` en `web.xml` (en una copia del WAR, sin commit) | Claude Code |
| 2026-11-22 | Ticket PDF `POST /galeno/api/publico/citas/{id}/ticket` con datos ficticios insertados solo en la base de prueba | Éxito: `200`, `Content-Type: application/pdf`, PDF 1.5 de 1 página (OpenPDF carga bien desde la librería global). El rol "Paciente" lo creó `CatalogoInicialService` al arrancar | Claude Code |
| _pendiente_ | HU1–HU4 desde el frontend en contenedor | | |

## Riesgos abiertos

- **PrimeFaces fuera del WAR (confirmado, bloqueante para JSF).** Las clases y el taglib se cargan
  desde la librería global, pero los componentes no se registran (`Undefined component type`).
  Las pantallas JSF no funcionan en el contenedor. El API REST y el ticket PDF sí. Hay que decidir
  una salida: (a) volver a meter PrimeFaces en el WAR, lo que incumple "todas las dependencias
  provided"; (b) declarar en el `faces-config.xml` de la app los componentes y renderers de
  PrimeFaces que se usan; (c) usar otra versión de PrimeFaces que los declare en XML.
- **Arranque lento por el ORB.** `CWWKS9660E` hace esperar ~30 s; la app arranca igual.
- **Duplicados en solicitudes simultáneas.** Las validaciones de unicidad son de la aplicación;
  dos guardados casi al mismo tiempo podrían colarse. Es aceptable con un solo calificador.
- **Versión de Open Liberty.** El `Dockerfile` usa la última por defecto; anotar aquí la que
  imprima el build.
