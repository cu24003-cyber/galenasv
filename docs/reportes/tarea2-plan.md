# Plan de la tarea 2: búsqueda pública de citas (HU1) y datos clínicos de desarrollo

Estado: **propuesta, pendiente de aprobación**. No se ha implementado nada.
Rama: `backend-rubrica`, sobre `4161c03`.

HU1 (rúbrica): asistente sin sesión con tipo de documento → número → clínica donde la
persona está registrada como paciente → rango de fechas. Muestra cards con fecha y hora,
clínica y procedimientos, y permite descargar un ticket PDF generado en el backend.
ChivoMed todavía no define el contrato (`vista_citas.js` es un marcador), así que este
plan lo fija.

## Lo que hay hoy

- La consulta guarda al paciente en `consulta.id_persona_rol`. La clínica es la de
  esa asignación (`persona_rol.id_clinica`). Un paciente registrado en dos clínicas
  tiene dos `persona_rol` PACIENTE.
- Los pasos se encadenan con `procedimiento_paso_secuencia` (`tipo_secuencia='SIGUIENTE'`,
  de `id_procedimiento_paso` a `id_procedimiento_paso_referencia`). El estado del paso
  es `PENDIENTE` o `COMPLETADO`, y `procedimiento_paso.indica_fin` marca el último.
- El responsable de cada paso ejecutado es `consulta_procedimiento_paso.id_persona_rol`,
  que debe ser una persona con el rol del paso en la misma clínica.
- `IntentosAcceso` es un único mapa en memoria de 10 intentos cada 5 minutos por IP, que
  hoy solo usa el login. El 429 ya sale con `Retry-After: 300` desde `ApiExceptionMapper`.
- `/api/publico/*` ya está excluido de la sesión en los dos filtros (tarea 1).

## 1. Datos clínicos de desarrollo

Archivo nuevo: `docs/migrations/20261112_datos_desarrollo_clinicos.sql`. Solo inserta
datos, sin cambios de esquema. Se aplica después de `20261111_datos_desarrollo.sql` y
`preparar-bd-desarrollo.sh` lo ejecuta como último paso. Todo usa UUID fijos y va en una
transacción con verificación final.

| Dato | Detalle |
| --- | --- |
| Segunda clínica | `de110000-…-000000000002`, "Clínica de demostración Norte", activa. |
| paciente.demo | Nueva `persona_rol` PACIENTE en la clínica 2. La de la clínica 1 ya existe. |
| doctor.demo | Nueva `persona_rol` DOCTOR en la clínica 2, necesaria como responsable de los pasos de esa clínica. Su fecha es posterior a la de la clínica 1, así que su rol inicial de login no cambia. |
| Procedimiento | "Consulta general de demostración": 1 "Toma de signos vitales" → 2 "Evaluación médica" → 3 "Indicaciones y cierre" (`indica_fin=true`). Los tres pasos tienen rol DOCTOR, con secuencias SIGUIENTE 1→2 y 2→3. |
| Consultas | Seis consultas de paciente.demo, tres por clínica: hace 10 a 30 días, hoy y en 3 a 7 días. Todas tienen el procedimiento. |
| Estados | Pasadas: los 3 pasos COMPLETADOS, con valor y `fecha_fin`. Hoy en la clínica 1: **paso 1 COMPLETADO con valor y paso 2 PENDIENTE** (responsable doctor.demo). Hoy en la clínica 2 y las futuras: paso 1 PENDIENTE, igual que deja `AtencionService` al iniciar un procedimiento. |
| Fechas | Relativas a `now()` y ancladas al día local: `(date_trunc('day', now() AT TIME ZONE 'America/El_Salvador') + interval '…') AT TIME ZONE 'America/El_Salvador'`. Así "hoy" coincide con la zona que muestra la app. |
| Textos | Valores y observaciones claramente ficticios ("Demostración: PA 120/80"); ningún dato real. |

Verificación final: dos clínicas activas con paciente.demo como PACIENTE; seis
consultas (una pasada, una de hoy y una futura por clínica); la consulta de hoy de la
clínica 1 tiene un paso COMPLETADO con valor y uno PENDIENTE. Si algo no cuadra, se
aborta sin escribir nada.

Lo verifico como en la tarea 1, con un PostgreSQL temporal sin docker: lo ejecuto dos
veces y compruebo los conteos y estados.

## 2. `GET /api/publico/clinicas?idTipoDocumento&numeroDocumento`

- Devuelve `200 [{idClinica, nombre}]`: clínicas activas donde la persona dueña del
  documento (tipo activo, valor exacto recortado) tiene un `persona_rol` con rol
  PACIENTE activo, ordenadas por nombre.
- Documento inexistente, documento de otro tipo y documento sin registro como
  paciente devuelven lo mismo: `200 []` (sección 6).
- Parámetros inválidos (falta alguno, UUID mal formado, número vacío, más de 50
  caracteres o con saltos de línea) dan 400, igual que el login. Un 400 no revela si
  el documento existe.

## 3. `GET /api/publico/citas?idTipoDocumento&numeroDocumento&idClinica&desde&hasta`

- `desde` y `hasta` son obligatorios, con formato `yyyy-MM-dd`, y se interpretan en
  `America/El_Salvador`: `[desde 00:00, hasta+1 00:00)`, es decir, `hasta` incluido.
  `hasta < desde` da 400. Un rango de más de 366 días da 400, para limitar el volumen
  por solicitud.
- Respuesta `200 [{idConsulta, fechaInicio, nombreClinica, procedimientos: [String]}]`,
  ordenada por `fechaInicio` ascendente. `fechaInicio` va en ISO-8601 con offset.
  `procedimientos` son los nombres sin repetir, en el orden de inicio. Uso
  `nombreClinica` igual que en `/auth/me`.
- **Nunca** incluye `observaciones`, `referenciaExterna`, `valor`, estados, responsables,
  órdenes ni resultados. El JSON se arma a mano con esos cuatro campos, como en
  `PublicoResource.tiposDocumento`.
- Si `idClinica` es un UUID válido pero la persona no es paciente ahí, devuelve `200 []`,
  igual que un documento inexistente.
- Una sola consulta JPQL filtra a la vez por documento, tipo activo, `persona_rol`
  PACIENTE de esa clínica y rango de fechas, y otra trae los nombres de procedimientos
  de los IDs resultantes. No hay un paso previo de "buscar persona" que responda antes
  cuando el documento no existe.

## 4. `GET /api/publico/citas/{idConsulta}/ticket?idTipoDocumento&numeroDocumento`

- **OpenPDF 3.0.5**: es `latest` y `release` en Maven Central (`com.github.librepdf:openpdf`,
  publicado el 2026-05-22). Requiere Java 21 y usa el paquete `org.openpdf.text`
  (desde la 3.0 ya no es `com.lowagie`). En el pom va con scope `compile`, para que quede
  dentro del WAR. icu4j, Bouncy Castle y FOP son opcionales y no se incluyen. `brotli4j`
  sí es obligatoria; al implementar la reviso con `mvn dependency:tree` y te aviso si arrastra
  binarios nativos.
- El PDF contiene el mismo resumen que la card: "GalenoSV – Ticket de cita", código de
  cita (`idConsulta`), fecha y hora en `America/El_Salvador`, clínica y lista de
  procedimientos. **No incluye** nombre, documento, notas, valores, responsables ni
  resultados. Fuentes estándar Helvetica, sin recursos externos.
- Responde `200`, `Content-Type: application/pdf` y
  `Content-Disposition: attachment; filename="ticket-cita-<primeros 8 del UUID>.pdf"`.
  El `no-store` ya lo pone `AutenticacionFilter`.
- Consulta inexistente, consulta de otra persona, documento inexistente o tipo inactivo
  dan **el mismo 404** `cita.noExiste`. La pertenencia se comprueba en la misma consulta
  JPQL de la sección 3, filtrando por `idConsulta`.
- Va en un `TicketCitaService` (control) separado del recurso.

## 5. Límite de intentos en las rutas públicas de búsqueda

- `IntentosAcceso` pasa a manejar ámbitos: `registrar(Ambito ambito, String ip)`, con la
  clave `ambito + ":" + ip` en el mismo mapa (se conservan la ventana, la limpieza y el
  tope de 10 000 entradas). `registrar(ip)` queda como atajo de `Ambito.LOGIN` (10 cada
  5 minutos) para que el login no cambie.
- `Ambito.BUSQUEDA_PUBLICA`: un **contador compartido** por `clinicas`, `citas` y
  `ticket`. Se registra antes de tocar la base, también si los parámetros son inválidos.
  Los límites están separados, así que buscar citas no bloquea el login de la misma IP.
- `tipos-documento` no cuenta: es un catálogo sin datos personales y el service worker lo
  guarda en caché.
- El 429 usa el código `publico.demasiadasSolicitudes` y el mensaje "Espere unos minutos
  antes de volver a buscar.". `Retry-After: 300` ya existe.
- Igual que en el login, es un límite en memoria por instancia, y detrás de un proxy
  todas las solicitudes comparten la IP. Lo documento.

## 6. Respuestas iguales para "documento inexistente" y "documento sin citas"

| Endpoint | Documento inexistente / otro tipo / tipo inactivo | Documento válido sin datos |
| --- | --- | --- |
| `clinicas` | `200 []` | `200 []` (no es paciente en ninguna clínica activa) |
| `citas` | `200 []` | `200 []` (sin citas en el rango o clínica ajena) |
| `ticket` | `404 cita.noExiste` | `404 cita.noExiste` (consulta ajena o inexistente) |

Las pruebas comparan el cuerpo completo de ambos casos, no solo el status.

## Pruebas JUnit

| Clase | Qué verifica |
| --- | --- |
| `IntentosAccesoTest` (se amplía) | Los ámbitos llevan contadores independientes (agotar el de búsqueda no bloquea el login de la misma IP y al revés); el límite de búsqueda y su ventana; `registrar(ip)` sigue contando como login. |
| `CitaPublicaRepositoryTest` (nuevo) | Cada JPQL recibe tipo, valor recortado, clínica, `idConsulta` y los límites de fecha como parámetros; filtra rol PACIENTE, clínica activa y tipo activo; no selecciona `observaciones`, `valor`, `referenciaExterna` ni el responsable. |
| `CitaPublicaServiceTest` (nuevo) | Conversión de `desde`/`hasta` a instantes de El Salvador con `hasta` incluido; 400 si el rango está invertido o pasa de 366 días; sin coincidencias devuelve lista vacía, sin excepción; agrupa los procedimientos sin repetir y en orden. |
| `PublicoResourceTest` (se amplía) | `clinicas` y `citas`: llaman primero al limitador; 429 sin consultar la base; 400 por parámetros inválidos; solo los campos documentados, y una lista de claves prohibidas (`observaciones`, `valor`, `estado`, `responsable`, `idPersonaRol`, `referenciaExterna`) no aparece en el JSON; **el cuerpo de "documento inexistente" es idéntico al de "documento sin citas"**. `ticket`: `application/pdf`, `Content-Disposition`, 404 idéntico para consulta ajena y documento inexistente. |
| `TicketCitaServiceTest` (nuevo) | El PDF empieza con `%PDF-`. Con `PdfTextExtractor` de OpenPDF se comprueba que contiene la fecha, la clínica y los procedimientos, y que **no** contiene un valor, una observación ni el nombre del médico usados como trampa en la entrada. |
| `PermisosApiHttpTest` / `AutenticacionFilterTest` | `publico/clinicas`, `publico/citas` y `publico/citas/{id}/ticket` responden sin sesión ni llamar a `auth.contexto`. |
| SQL temporal | Los datos clínicos se aplican dos veces, con sus conteos y estados. |

Como en la tarea 1, el JPQL se prueba por su texto y parámetros, porque las pruebas no
tienen JPA real. Lo dejo anotado para probarlo en Liberty.

## Documentación

- `docs/api-rest.md`: los tres endpoints con parámetros, ejemplo de respuesta, errores,
  el límite por IP y la tabla de respuestas iguales.
- `openapi.json`: rutas, esquemas `ClinicaPublica` y `CitaPublica`, y respuesta
  `application/pdf`.
- `docs/autenticacion.md`: se amplía "Rutas públicas" con el límite por ámbitos.

## Commits propuestos (`mvn clean test` antes de cada uno y `git status` previo)

1. Datos clínicos de desarrollo y el paso nuevo del script.
2. `IntentosAcceso` con ámbitos.
3. Repositorio y servicio de búsqueda pública (`CitaPublicaRepository`, `CitaPublicaService`).
4. `GET /api/publico/clinicas` y `GET /api/publico/citas`.
5. OpenPDF, `TicketCitaService` y `GET /api/publico/citas/{id}/ticket`.
6. Documentación (`api-rest.md`, `autenticacion.md`, `openapi.json`).

Al final escribo el reporte en `docs/reportes/tarea2-reporte.md`.

## Decisiones que necesito

1. **Límite de búsqueda pública.** Propongo **20 solicitudes cada 5 minutos por IP**,
   compartidas entre `clinicas`, `citas` y `ticket`. Alcanza para un asistente completo
   con varias búsquedas y descargas, y deja a una IP en unos 5 700 intentos al día. El
   login sigue en 10.
2. **Qué pasa al volver a ejecutar los datos clínicos.** Recomiendo que vuelva a calcular
   las fechas de las seis consultas y sus pasos a partir de `now()`, sin tocar los estados
   ni borrar nada (CLAUDE.md prohíbe borrar datos). Si alguien avanzó la consulta de hoy
   desde la app, sigue avanzada; para volver a tener una a medio avanzar hay que crear la
   base desde cero. La alternativa es no tocar nada si ya existe (`DO NOTHING`), pero
   entonces "hoy" deja de ser hoy al día siguiente.
3. **El documento viaja en la URL.** Las tres rutas son GET, como pediste, y el número de
   documento queda en la query string. Puede aparecer en logs de acceso o proxies (Liberty
   no registra accesos por defecto) y en el historial si el ticket se abre como enlace. Mi
   recomendación es mantener GET, que se descarga con `fetch` y blob sin dejar historial, y
   documentar que no se active el access log con query string. La alternativa es pasar
   `citas` y `ticket` a POST con cuerpo JSON. ¿Lo mantengo en GET?
