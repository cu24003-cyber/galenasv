# Reporte de la tarea 2: búsqueda pública de citas (HU1) y datos clínicos

Fecha: 2026-11-12. Rama: `backend-rubrica`. Base: `4161c03`. Plan:
`docs/reportes/tarea2-plan.md`, aprobado con cambios. Resultado: 7 commits y
`mvn clean package` con **634 pruebas, 0 fallos, 0 errores, 0 omitidas** (antes: 602).

## Cambios aprobados respecto al plan

| Cambio pedido | Cómo quedó |
| --- | --- |
| Límite de 60 por IP cada 5 minutos, compartido | `IntentosAcceso.Ambito.BUSQUEDA_PUBLICA` (60) separado de `LOGIN` (10). |
| Modo de reinicio de la demo | `psql -v reiniciar=1` sobre el mismo SQL de datos, o `scripts/reiniciar-demo.sh`. |
| POST con el documento en el cuerpo | Las tres rutas son POST. Ningún dato del documento viaja en la URL; solo `idConsulta` en la ruta del ticket. |
| OpenPDF 2.x si la 3 agrega nativos | **Quedó OpenPDF 2.4.0** (ver abajo). |
| doctor.demo con ADMINISTRACION en la clínica 1 | Hecho. Su rol inicial de login sigue siendo DOCTOR en la clínica 1. |

## Por qué OpenPDF 2.4.0

OpenPDF 3.0.5 es la última versión (2026-05-22), pero su dependencia obligatoria
`brotli4j 1.23.0` agrega `native-linux-x86_64-1.23.0.jar` al WAR, con
`lib/linux-x86_64/libbrotli.so` (976 KB). Es un binario nativo y solo funciona en
Linux x86_64. Lo comprobé con `mvn dependency:tree` y listando `WEB-INF/lib`.

OpenPDF **2.4.0** es la última de la rama 2.x (2025-08-08). Es un solo JAR sin dependencias
obligatorias (icu4j, Bouncy Castle y FOP son opcionales), compilado para Java 21 (class
major 65), y su paquete es `com.lowagie.text`. Con ella, el WAR final solo agrega
`openpdf-2.4.0.jar` y la revisión de `.so/.dll/.dylib/.jnilib` en `WEB-INF/lib` no encontró
ninguno.

## Commits

| Commit | Contenido | `mvn clean test` |
| --- | --- | --- |
| `ee83044` | Datos clínicos demo relativos a la fecha y su prueba SQL; paso nuevo en `preparar-bd-desarrollo.sh` | 602 / 0 fallos |
| `4bbc298` | Modo de reinicio y `scripts/reiniciar-demo.sh` | 602 / 0 fallos |
| `bc9b185` | `IntentosAcceso` por ámbitos (LOGIN 10, BUSQUEDA_PUBLICA 60) | 605 / 0 fallos |
| `15cba36` | `CitaPublicaRepository` (proyecciones) y `CitaPublicaService` | 618 / 0 fallos |
| `66dafe3` | `POST /api/publico/clinicas` y `POST /api/publico/citas` | 628 / 0 fallos |
| `2d113cb` | OpenPDF 2.4.0, `TicketCitaService` y `POST /api/publico/citas/{id}/ticket` | 634 / 0 fallos |
| `74f898f` | `api-rest.md`, `autenticacion.md`, `openapi.json` | 634 / 0 fallos |

Antes de cada commit se mostró `git status` con solo los archivos de ese paso.
`nbactions.xml`, `generar_tests.sh`, `inyectar_*.py` y `docs/reportes/` siguen fuera
de los commits. No se hizo push.

## Rutas

| Ruta | Cuerpo | Respuesta |
| --- | --- | --- |
| `POST /api/publico/clinicas` | `{idTipoDocumento, numeroDocumento}` | `200 [{idClinica, nombre}]` |
| `POST /api/publico/citas` | `{idTipoDocumento, numeroDocumento, idClinica, desde, hasta}` | `200 [{idConsulta, fechaInicio, nombreClinica, procedimientos}]` |
| `POST /api/publico/citas/{idConsulta}/ticket` | `{idTipoDocumento, numeroDocumento}` | `200 application/pdf`, adjunto `ticket-cita-<8>.pdf` |

- Sin sesión ni CSRF. Como cualquier escritura, exigen un `Origin`/`Referer` permitido
  (en curl: `-H 'Origin: http://localhost:9080'`).
- Documento inexistente y documento sin datos: `200 []` en `clinicas` y `citas`, y el
  mismo `404 cita.noExiste` en `ticket`. La pertenencia se resuelve en la misma consulta
  JPQL que filtra por documento, así que no hay un paso previo que responda antes cuando el
  documento no existe.
- `desde`/`hasta` son días `yyyy-MM-dd` de El Salvador, con `hasta` incluido; un rango
  invertido o de más de 366 días da 400 `citas.rango`. `fechaInicio` se devuelve con offset
  `-06:00`.
- Cada solicitud cuenta en el límite antes de validar el cuerpo. Pasadas 60 en 5 minutos:
  `429 publico.demasiadasSolicitudes` con `Retry-After: 300`.
- El ticket contiene solo código, fecha y hora, clínica y procedimientos, más el título y una
  nota fija. Lo revisé generando uno y abriéndolo: A6 con acentos correctos.

## Datos clínicos de demostración

`docs/migrations/20261112_datos_desarrollo_clinicos.sql` (UUID fijos `de11000x-…`):

| Dato | Detalle |
| --- | --- |
| Clínicas | "Clínica de demostración" (1, ya existía) y "Clínica de demostración Norte" (2). |
| paciente.demo (DUI `00000000-3`) | PACIENTE en ambas clínicas. |
| doctor.demo (DUI `00000000-2`) | DOCTOR en ambas y ADMINISTRACION en la clínica 1. Las asignaciones nuevas se fechan 1 y 2 minutos después de su DOCTOR de la clínica 1, que sigue siendo el rol inicial. |
| Procedimiento | "Consulta general de demostración": Toma de signos vitales → Evaluación médica → Indicaciones y cierre (`indica_fin`). Los tres pasos DOCTOR, con secuencias SIGUIENTE. |
| Consultas `de110009-…-1..6` | Clínica 1: hace 20 días, hoy 08:00 y en 7 días. Clínica 2: hace 12 días, hoy 11:00 y en 3 días. Horas de El Salvador. |
| Estados | Pasadas: 3 pasos COMPLETADOS con valor. **Hoy en la clínica 1 (`…-2`): paso 1 COMPLETADO con valor y paso 2 PENDIENTE.** Las demás: paso 1 PENDIENTE. |

- **Carga normal:** si se repite, inserta lo que falte y ancla de nuevo las fechas a hoy,
  desplazando consulta, procedimientos y pasos (también los creados en la app) sin cambiar
  estados.
- **Reinicio (`reiniciar=1`):** borra solo los resultados, órdenes, pasos y procedimientos que
  cuelgan de las seis consultas demo y no forman parte de su estado inicial. Luego restaura
  fechas, estados, valores, notas y referencia, y verifica que queden exactamente los pasos
  iniciales; si no, revierte todo. Esto es un borrado de datos, que CLAUDE.md prohíbe en
  general, y se hizo porque lo pediste explícitamente. El script pide confirmación.

### Verificación SQL

En un PostgreSQL local temporal (sin docker), que se borró al terminar:

- Carga normal ejecutada dos veces: la prueba `docs/migrations/tests/datos_desarrollo_clinicos_test.sql` pasa.
- Con las consultas desplazadas 3 días y el paso 2 completado a mano, la carga normal devolvió las fechas a hoy y mantuvo el paso 2 COMPLETADO.
- Reinicio sobre una base "sucia" (paso 3 extra, orden y resultado, procedimiento extra, consulta cerrada con referencia): quedaron 0 extras, la consulta abierta sin referencia y la prueba SQL pasa. Una consulta ajena a la demo quedó intacta.
- Reinicio sobre una base sin datos clínicos previos: carga todo y la prueba pasa.
- SQL equivalente a las consultas JPQL sobre la demo: clínicas de `00000000-3` son las dos; de `00000000-2` (doctor), 0; citas de la clínica 1 en ±30 días son 3 (pasada, hoy 08:00 y futura); procedimientos de la consulta de hoy: "Consulta general de demostración".

## Pruebas JUnit agregadas o ampliadas

| Clase | Qué verifica |
| --- | --- |
| `IntentosAccesoTest` (4) | 60 por ventana con su código; ámbitos independientes en ambos sentidos; `registrar(ip)` sigue siendo LOGIN. |
| `CitaPublicaRepositoryTest` (6) | Filtros de documento, tipo activo, PACIENTE y clínica activa; parámetros, nunca concatenación; el SELECT de cada consulta no contiene campos clínicos ni de persona; entradas vacías no consultan. |
| `CitaPublicaServiceTest` (7) | Rango en hora de El Salvador con `hasta` incluido; 400 por rango invertido, mayor de 366 días o nulo; vacío sin consultar procedimientos; procedimientos sin repetir y en orden. |
| `PublicoResourceTest` (13) | Campos exactos de `clinicas` y `citas`; lista de campos prohibidos; fecha con offset `-06:00`; **cuerpo idéntico para documento inexistente y sin datos**; 429 sin tocar la base; cuerpos inválidos dan 400 y cuentan como intento; ticket con `application/pdf` y `Content-Disposition`; **mismo 404 para cita ajena, documento inexistente e id inválido**. |
| `TicketCitaServiceTest` (3) | El PDF empieza con `%PDF-`, tiene una página y su texto, extraído con OpenPDF, es **exactamente** la lista de líneas esperada, sin datos extra; caso sin procedimientos; nombre de archivo. |
| `AutenticacionFilterTest` | POST a `publico/clinicas` y `publico/citas` sin sesión ni CSRF; un origen ajeno da 403. |
| `PermisosApiHttpTest` | Las tres rutas POST pasan sin llamar a `auth.contexto`. |

## Comandos

Variables usadas abajo:

```bash
API=http://localhost:9080/galenosv/api
ORIGEN='Origin: http://localhost:9080'
DUI=de110004-0000-4000-8000-000000000001
C1=de110000-0000-4000-8000-000000000001
C2=de110000-0000-4000-8000-000000000002
HOY=$(TZ=America/El_Salvador date +%F)
DESDE=$(TZ=America/El_Salvador date -d '-30 days' +%F)
HASTA=$(TZ=America/El_Salvador date -d '+30 days' +%F)
```

### Cargar y reiniciar los datos

```bash
mvn -DskipTests package
bash scripts/preparar-bd-desarrollo.sh [contenedor] [base] [usuario_postgres]   # todo desde cero o completar
bash scripts/reiniciar-demo.sh [contenedor] [base] [usuario_postgres]          # pide confirmación
# Sin los scripts (psql directo):
psql -X -v ON_ERROR_STOP=1 -d galenasv -f docs/migrations/20261112_datos_desarrollo_clinicos.sql
psql -X -v ON_ERROR_STOP=1 -d galenasv -v reiniciar=1 -f docs/migrations/20261112_datos_desarrollo_clinicos.sql
psql -X -v ON_ERROR_STOP=1 -d galenasv -f docs/migrations/tests/datos_desarrollo_clinicos_test.sql
```

### Probar cada ruta

```bash
# Tipos de documento (GET, sin cuerpo)
curl -s "$API/publico/tipos-documento"

# Clínicas del paciente: espera las dos clínicas demo
curl -s -X POST "$API/publico/clinicas" -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\"}"

# Documento inexistente y documento sin registro como paciente: ambos devuelven []
curl -s -X POST "$API/publico/clinicas" -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"99999999-9\"}"
curl -s -X POST "$API/publico/clinicas" -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-2\"}"

# Citas en la clínica 1, ±30 días: espera 3 (pasada, hoy 08:00 y futura)
curl -s -X POST "$API/publico/citas" -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\",\"idClinica\":\"$C1\",\"desde\":\"$DESDE\",\"hasta\":\"$HASTA\"}"

# Solo hoy en la clínica 2: espera 1 (11:00)
curl -s -X POST "$API/publico/citas" -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\",\"idClinica\":\"$C2\",\"desde\":\"$HOY\",\"hasta\":\"$HOY\"}"

# Rango invertido: 400 citas.rango
curl -s -X POST "$API/publico/citas" -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\",\"idClinica\":\"$C1\",\"desde\":\"$HASTA\",\"hasta\":\"$DESDE\"}"

# Ticket de la consulta de hoy (clínica 1): guarda el PDF y muestra las cabeceras
curl -s -D - -o ticket.pdf -X POST "$API/publico/citas/de110009-0000-4000-8000-000000000002/ticket" \
  -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\"}"

# Ticket con el documento de otra persona: 404 cita.noExiste (igual que con un documento inexistente)
curl -s -X POST "$API/publico/citas/de110009-0000-4000-8000-000000000002/ticket" \
  -H "$ORIGEN" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-2\"}"

# Sin Origin: 403 auth.origen
curl -s -X POST "$API/publico/clinicas" -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\"}"

# Límite: la solicitud 61 en 5 minutos devuelve 429 con Retry-After: 300
for i in $(seq 1 61); do
  curl -s -o /dev/null -w '%{http_code}\n' -X POST "$API/publico/clinicas" -H "$ORIGEN" \
    -H 'Content-Type: application/json' -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\"}"
done | sort | uniq -c
```

El límite se guarda en memoria: después de probarlo hay que esperar 5 minutos o reiniciar
Liberty.

## Limitaciones y pendientes

- **No se probó contra Liberty.** No levanté el servidor porque usaría tu base en docker. El
  JPQL se verificó por su texto y parámetros, y su lógica con SQL equivalente sobre la demo,
  pero no con EclipseLink. Lo primero a comprobar son los curl de arriba. Los puntos con más
  riesgo:
  - Las proyecciones `Object[]` con `JOIN` sobre las relaciones `@ManyToOne`.
  - El `IN :consultas` con una lista de UUID.
  - La deserialización de `LocalDate` en el cuerpo (Yasson).
- `scripts/preparar-bd-desarrollo.sh` y `scripts/reiniciar-demo.sh` no se ejecutaron, porque
  usan docker. Se comprobó su sintaxis y el SQL que ejecutan.
- El límite por IP vive en memoria por instancia, y detrás de un proxy todas las solicitudes
  comparten la IP.
- El reinicio no deshace cambios en el catálogo del procedimiento demo: si alguien agrega un
  paso al procedimiento desde la app, ese paso se conserva. Las consultas sí vuelven a su
  estado inicial.
- ChivoMed todavía no consume estas rutas (`vista_citas.js` es un marcador). El contrato
  quedó en `docs/api-rest.md` y `openapi.json`.
- Ni este reporte ni los de la tarea 1 están en un commit.
