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

## Evidencia de verificación

| Fecha | Qué se verificó | Resultado | Quién |
| --- | --- | --- | --- |
| 2026-11-22 | `mvn clean test` después de `6cb2cf7`–`8c835a3` | 832 pruebas, 0 fallos, 0 errores (antes: 815) | Junior (reportado) |
| 2026-11-22 | `unzip -l target/galeno.war \| grep WEB-INF/lib` | Sin salida (WAR sin librerías) | Junior (reportado) |
| 2026-11-22 | Las 20 entidades contra `01-original.sql` + `02-parche.sql` | Ninguna mapea columnas inexistentes; las longitudes 20 y 155 coinciden | Revisión estática |
| 2026-11-22 | Los `DEFAULT` del esquema (`fecha_creacion`, `activo`, etc.) | 12 entidades los asignan con `@PrePersist` | Revisión estática |
| _pendiente_ | `docker build` de la imagen backend | | |
| _pendiente_ | Arranque con el comando del profesor sobre la base vacía (`docker/README.md`, pasos 2 y 3) | | |
| _pendiente_ | Una pantalla JSF (PrimeFaces desde la librería global) y el ticket PDF (OpenPDF) | | |
| _pendiente_ | HU1–HU4 desde el frontend en contenedor | | |

## Riesgos abiertos

- **PrimeFaces fuera del WAR.** Se carga desde la librería global de Liberty. Si las pantallas JSF
  fallan, esa es la primera sospecha; se debe confirmar en la prueba pendiente.
- **Duplicados en solicitudes simultáneas.** Las validaciones de unicidad son de la aplicación;
  dos guardados casi al mismo tiempo podrían colarse. Es aceptable con un solo calificador.
- **Versión de Open Liberty.** El `Dockerfile` usa la última por defecto; anotar aquí la que
  imprima el build.
