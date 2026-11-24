# Levantar el backend de GalenoSV desde cero

Guía para dejar el backend (`galenasv`) funcionando en otra computadora para la revisión:

- Open Liberty en `http://localhost:9080/galeno`, con la API en `/galeno/api`;
- PostgreSQL en Docker, con los datos demo del guion del Primer Parcial para las HU1 a HU4.

Todos los comandos se ejecutan **desde la raíz del repositorio `galenasv`**, salvo que se indique
otra cosa.

## 1. Requisitos

| Herramienta | Versión | Comprobar con |
| --- | --- | --- |
| JDK | **21 o posterior** (el `pom.xml` compila con `release 21`) | `java -version` |
| Maven | 3.x reciente (verificado con 3.10.0) | `mvn -v` |
| Docker | Cualquier versión actual | `docker version` |
| PostgreSQL (imagen de Docker) | **17 o 18** (el mínimo técnico es 13) | `docker exec galenasv psql -U postgres -tAc 'show server_version'` |
| curl | Para las verificaciones | `curl --version` |

La primera ejecución necesita Internet: Maven descarga las dependencias y el runtime de Open
Liberty. No hace falta instalar Liberty ni el driver JDBC: el driver está versionado en
`src/main/liberty/config/lib/global/postgresql-42.7.5.jar`.

## 2. Contenedor de PostgreSQL

Los scripts usan por defecto el contenedor `galenasv`, la base `galenasv` y el usuario
`postgres`. Si se usan esos nombres, los scripts se ejecutan sin argumentos. Cambie
`CLAVE_LOCAL` por una contraseña propia.

PostgreSQL 18:

```bash
docker run -d --name galenasv \
  -e POSTGRES_PASSWORD=CLAVE_LOCAL \
  -p 5432:5432 \
  -v galenasv-datos:/var/lib/postgresql \
  postgres:18
```

PostgreSQL 17 (la imagen guarda los datos en otra ruta):

```bash
docker run -d --name galenasv \
  -e POSTGRES_PASSWORD=CLAVE_LOCAL \
  -p 5432:5432 \
  -v galenasv-datos:/var/lib/postgresql/data \
  postgres:17
```

Esperar a que acepte conexiones:

```bash
docker exec galenasv pg_isready -U postgres
# /var/run/postgresql:5432 - accepting connections
```

La base **no** se crea a mano: la crea `scripts/preparar-bd-desarrollo.sh` en el paso 5. En los
días siguientes basta con `docker start galenasv`; los datos quedan en el volumen
`galenasv-datos`.

## 3. Configurar Liberty con variables de entorno

Copiar la plantilla sin secretos y crear el archivo privado de configuración:

```bash
cp src/main/liberty/config/server.xml.ejemplo src/main/liberty/config/server.xml
cp .env.example .env
chmod 600 .env
```

Si ya tiene `.env`, edítelo sin sobrescribirlo. Complete `PG_USER`, `PG_PASS` y
`LTPA_KEYS_PASSWORD` con sus valores locales. Conserve la clave LTPA del entorno existente;
cambiarla puede invalidar tokens. Para un entorno nuevo puede generar una con
`openssl rand -hex 32`.

| Variable | Uso | Origen |
| --- | --- | --- |
| `PG_USER` | Usuario PostgreSQL | Rúbrica, obligatoria |
| `PG_PASS` | Contraseña PostgreSQL | Rúbrica, obligatoria |
| `PG_DB` | Base de datos | Rúbrica, obligatoria |
| `PG_HOST` | Host/IP PostgreSQL | Rúbrica, obligatoria |
| `PG_PORT` | Puerto PostgreSQL | Rúbrica, obligatoria |
| `LTPA_KEYS_PASSWORD` | Protección de claves LTPA | `server.xml`, obligatoria |
| `HTTP_PORT` | Puerto HTTP; predeterminado 9080 | `server.xml`, opcional |
| `HTTPS_PORT` | Puerto HTTPS; predeterminado 9443 | `server.xml`, opcional |
| `HTTP_HOST` | Interfaz HTTP; predeterminado localhost | Adicional para contenedores |

`.env` usa el formato literal de `docker --env-file`: una línea `NOMBRE=valor`, sin
`export`, sin comillas envolventes y sin comentarios al final del valor. Los espacios,
`$`, `#`, `=` y comillas dentro del valor se conservan; no se ejecutan expresiones.
No admite valores con saltos de línea. Las variables ya definidas en el proceso tienen
prioridad. `bash scripts/liberty.sh` carga `.env`, comprueba las seis variables obligatorias
y ejecuta Maven; admite argumentos como `liberty:dev` o `liberty:run`.

Open Liberty no carga automáticamente un archivo llamado `.env`. El XML referencia las
variables del proceso mediante `${env.PG_USER}`, etc.; HTTP usa variables con valor
predeterminado. Si arranca desde el IDE o con `mvn liberty:run` directamente, configure
esas variables en el entorno del proceso. Véase la
[documentación oficial de Liberty](https://openliberty.io/docs/latest/reference/config/server-configuration-overview.html).

`.env` y `.env.*` están excluidos de Git; `.env.example` contiene solo valores de ejemplo.
`.dockerignore` excluye los secretos y `target/` del contexto de construcción. No copie
`.env` al WAR, a un ZIP de entrega ni a una imagen. El `server.xml` local sigue excluido
de Git; la plantilla versionada permite reproducir la configuración.

Para la futura imagen, pase `--env-file .env` y `-e 'HTTP_HOST=*'` a `docker run`.
Use en `PG_HOST` el nombre/IP de PostgreSQL accesible desde el contenedor: `localhost`
dentro del contenedor apunta al propio contenedor. Si solo pasa las cinco variables
PostgreSQL de la rúbrica, agregue también `LTPA_KEYS_PASSWORD` para esta configuración.
La imagen deberá copiar la plantilla como `server.xml` y arrancar Liberty con esas
variables; no necesita el script Maven si ya incluye el WAR y el runtime.

Tercer parcial: el WAR se llama `galeno.war` (contexto `/galeno`) y OpenPDF, PostgreSQL,
PrimeFaces y PrimeFaces Themes tienen alcance `provided`, así que `WEB-INF/lib` va vacío.
El servidor debe aportar esos JAR (por ejemplo en `lib/global` de Liberty, que es una
biblioteca compartida global) o la aplicación no encontrará PrimeFaces ni OpenPDF. El
proxy del frontend debe apuntar a `/galeno/api`. La imagen Docker sigue pendiente.

## 4. Compilar

```bash
mvn -DskipTests package
```

Para correr las pruebas: `mvn test`. No necesita la base ni Liberty.
La carga de variables se comprueba con `python3 scripts/test-liberty-env.py`, sin arrancar servicios.

## 5. Crear la base y cargar los datos demo

```bash
bash scripts/preparar-bd-desarrollo.sh [contenedor] [base] [usuario_postgres]
```

| Argumento | Por defecto |
| --- | --- |
| `contenedor` | `galenasv` |
| `base` | `galenasv` |
| `usuario_postgres` | `postgres` |

Con los nombres del paso 2 basta con `bash scripts/preparar-bd-desarrollo.sh`. Con otros:

```bash
bash scripts/preparar-bd-desarrollo.sh mi-contenedor galenasv mi_usuario
```

El script hace, en este orden y dentro del contenedor (`docker exec … psql`):

1. crea la base si no existe;
2. aplica `docs/migrations/20261111_base_completa.sql`;
3. aplica `20261112_documento_tipo_valor_unico.sql`;
4. aplica `20261113_orden_examen_examen.sql`, columna histórica usada por los fixtures demo;
5. aplica `20261115_login_por_documento.sql`, que elimina la tabla `usuario`;
6. carga `20261113_datos_demo_rubrica.sql`: los datos demo del guion del Primer Parcial
   (`docs/datos-demo-rubrica.md`): clínicas, roles, tipos, exámenes, procedimientos, personas,
   documentos, contactos y asignaciones;
7. carga `20261113_datos_demo_rubrica_consultas.sql`: las 11 consultas demo con sus pasos y órdenes;
8. aplica `20261121_ordenes_base_original.sql` para conservar esas asociaciones en el formato actual. Ejecutar con el backend detenido. Estos scripts son solo para desarrollo; no para la base original del ingeniero.

Debe terminar con `Base preparada: galenasv. …`. **Se puede repetir sin error** y deja los mismos
datos (se verificó cargando dos veces). No borra datos. Para comprobar lo cargado:

```bash
docker exec -i galenasv psql -X -v ON_ERROR_STOP=1 -U postgres -d galenasv \
  < docs/migrations/tests/datos_demo_rubrica_test.sql
# Datos demo del guion correctos.
```

Si la base ya tenía la demo anterior (admin.demo, doctor.demo, paciente.demo), siga antes la
sección 5.1.

### 5.1. Si la base ya tiene la demo anterior

Hasta la B8 la demo era "Clínica de demostración" con admin.demo, doctor.demo y paciente.demo
(UUID `de110000…`). La demo nueva no la borra: se quita con un script aparte, que **no** ejecuta
`preparar-bd-desarrollo.sh`. Haga los pasos en este orden. En los ejemplos, `CONTENEDOR`, `BASE` y
`USUARIO` son los mismos argumentos del script (por defecto `galenasv`, `galenasv`, `postgres`).

1. **Respaldo** de la base completa, fuera del contenedor, y comprobación de que se puede leer:
   ```bash
   mvn -DskipTests package
   RESPALDO=respaldo-galenasv-$(date +%Y%m%d-%H%M%S).dump
   docker exec CONTENEDOR pg_dump -U USUARIO -d BASE -Fc > "$RESPALDO"
   docker exec -i CONTENEDOR pg_restore -l < "$RESPALDO" | head -5
   ```
2. **Limpieza** de la demo anterior. Borra sus clínicas, personas, procedimiento,
   exámenes y consultas, incluido lo que se les agregó desde la app. Conserva el tipo de
   documento DUI. Si alguna fila de otra persona depende de la demo (por ejemplo, una asignación
   en "Clínica de demostración"), se detiene **sin borrar nada** y dice cuántas hay:
   ```bash
   docker exec -i CONTENEDOR psql -X -v ON_ERROR_STOP=1 -U USUARIO -d BASE \
     < docs/migrations/20261113_limpieza_demo_anterior.sql
   docker exec -i CONTENEDOR psql -X -v ON_ERROR_STOP=1 -U USUARIO -d BASE \
     < docs/migrations/tests/datos_demo_rubrica_limpieza_test.sql
   # Demo anterior eliminada.
   ```
3. **Datos nuevos:**
   ```bash
   bash scripts/preparar-bd-desarrollo.sh CONTENEDOR BASE USUARIO
   ```
4. **Prueba SQL** de los datos nuevos:
   ```bash
   docker exec -i CONTENEDOR psql -X -v ON_ERROR_STOP=1 -U USUARIO -d BASE \
     < docs/migrations/tests/datos_demo_rubrica_test.sql
   # Datos demo del guion correctos.
   ```

Si algo sale mal, se vuelve al estado del respaldo con:

```bash
docker exec -i CONTENEDOR pg_restore -U USUARIO -d BASE --clean --if-exists --single-transaction < "$RESPALDO"
```

## 6. Arrancar Liberty

```bash
bash scripts/liberty.sh
```

Deja el servidor en primer plano; se detiene con `Ctrl+C`. La primera vez descarga Open Liberty
y la feature `jakartaee-11.0`, así que tarda varios minutos. Al recompilar (`mvn compile`,
`mvn test` o `mvn package`), Liberty recarga la aplicación solo: aparece `CWWKZ0003I` en el log.

## 7. Verificar que está arriba

1. **Log de Liberty.** En la consola o en
   `target/liberty/wlp/usr/servers/galenosv/logs/messages.log` deben aparecer:
   ```text
   CWWKT0016I: Web application available (default_host): http://localhost:9080/galeno/
   CWWKF0011I: The galenosv server is ready to run a smarter planet. ...
   ```
   No debe haber líneas `E ` (error) de JPA o del dataSource.
2. **API pública, sin sesión:**
   ```bash
   curl -s http://localhost:9080/galeno/api/publico/tipos-documento
   # [{…,"nombre":"DUI"},{…,"nombre":"Pasaporte"}]
   ```
   Debe listar DUI y Pasaporte (Licencia de conducir está inactiva); si responde `[]`, faltan los
   datos del paso 5. El `idTipoDocumento` del DUI depende de la base: es `de120003-…-1` en una
   base nueva y el UUID que ya tenía el DUI en una base existente (por ejemplo `de110004-…-1`).
3. **Login de un empleado** (Carlos Gómez, Doctor). Responde `204` y crea la sesión; la cabecera
   `Origin` es obligatoria, ver "Errores comunes". El ejemplo toma el UUID del DUI con `jq`:
   ```bash
   DUI=$(curl -s http://localhost:9080/galeno/api/publico/tipos-documento | jq -r '.[] | select(.nombre=="DUI") | .idTipoDocumento')
   curl -s -o /dev/null -w '%{http_code}\n' -c /tmp/galenosv.cookies \
     -H 'Origin: http://localhost:3000' -H 'Content-Type: application/json' \
     -X POST http://localhost:9080/galeno/api/auth/login \
     -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"01000001-1\"}"
   # 204
   curl -s -b /tmp/galenosv.cookies http://localhost:9080/galeno/api/atencion/dashboard
   # [ … consultas de hoy de Clínica Santa Ana … ]
   ```

`GET /api/jakartaee11` **no** sirve para comprobar el servidor: exige sesión y sin ella responde
401.

## 8. Frontend (ChivoMed) en el puerto 3000

El frontend se levanta en su repositorio con `npm start`, que sirve en `http://localhost:3000`
y debe reenviar su ruta de API a `http://localhost:9080/galeno/api` (su proxy aún usa `/galenosv`). **Debe correr en el puerto
3000**:

- el backend solo acepta escrituras (login, cambio de rol, acciones) cuyo `Origin` sea el propio
  backend o uno de la variable de entorno `CHIVOMED_ORIGINS`;
- sin esa variable, se aceptan `http://127.0.0.1:3000` y `http://localhost:3000`.

Para usar otro puerto u otro host, definir la variable **antes** de arrancar Liberty, en la misma
terminal. Reemplaza los valores por defecto, así que incluya también los que quiera conservar:

```bash
CHIVOMED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000,http://localhost:3001 bash scripts/liberty.sh
```

## 9. Antes de la revisión: reiniciar la demo

Las consultas demo tienen fechas **relativas a hoy**: una consulta de hoy a medio avanzar, otras
pasadas y otras futuras. Las pruebas manuales las modifican. El **mismo día de la revisión**, con
el contenedor arriba, ejecute:

```bash
bash scripts/reiniciar-demo.sh [contenedor] [base] [usuario_postgres]
```

Usa los mismos argumentos y valores por defecto que `preparar-bd-desarrollo.sh` y pide
confirmación (`s`).

- **Qué hace:** vuelve a anclar las 11 consultas demo al día de hoy y les devuelve su estado
  inicial. Borra los resultados, órdenes, pasos y procedimientos agregados desde la app **solo**
  a esas consultas, y restaura las dos órdenes demo (Hemograma completo en SA-1 y Glucosa en
  ayunas en SA-6).
- **Qué no toca:** ningún otro dato.
- **No hace falta reiniciar Liberty.**

Para comprobarlo, la misma prueba SQL del paso 5 (`tests/datos_demo_rubrica_test.sql`).

## 10. Datos demo

Son los del guion del Primer Parcial; el detalle completo está en `docs/datos-demo-rubrica.md`.
Todos los datos son inventados. Empleado es cualquier rol activo excepto Paciente; cada paso lo
atiende quien tiene su rol en la clínica, y cierra la consulta quien completó el paso de
finalización (docs/flujo-consulta.md).

| Documento | Persona | Asignaciones (rol y clínica) | Login y uso |
| --- | --- | --- | --- |
| DUI `01000001-1` o Pasaporte `A10000001` | Carlos Gómez | Doctor en Santa Ana (**rol inicial**) y en San Salvador | Entra (HU2); selector con 2 clínicas (HU3); atiende la Consulta médica de SA-3 y SS-2 (HU4). |
| DUI `01000002-2` | Lucía Pérez | Enfermera en Santa Ana | Entra; atiende el Triaje de SA-5 y la Toma de presión de SA-4. |
| DUI `01000003-3` | Marta López | Atención al público en Santa Ana y en San Salvador | Entra; completa la Entrega de indicaciones de SA-6 y cierra la consulta. |
| DUI `01000006-6` | Rosa Méndez | Enfermera en San Salvador | Entra. |
| DUI `01000007-7` | Jorge Ruiz | Doctor solo en San Salvador | Entra. |
| DUI `01000004-4` o Pasaporte `B10000004` | Ana Martínez | Paciente en Santa Ana y en San Salvador | Entra; su rol PACIENTE no permite abrir páginas de personal. HU1: citas pasadas, de hoy y futuras en 2 clínicas. |
| DUI `01000008-8`, `01000009-9` | Julia Flores, Juan Vásquez | Paciente en Santa Ana | Entran; solo tienen acceso de paciente. |
| DUI `01000005-5` | Pedro Hernández | Ninguna | Entra; sin rol no puede abrir páginas protegidas. |
| `12345678-9` | Nadie | — | HU1 y HU2: mensaje neutro. |

El login usa el tipo y número de documento. El rol sigue determinando las páginas y operaciones
disponibles.

**Procedimientos** (cada paso con un rol distinto; el último es la finalización):

- Consulta general: Triaje (Enfermera) → Consulta médica (Doctor) → Entrega de indicaciones
  (Atención al público).
- Control de hipertensión: Toma de presión arterial (Enfermera) → Evaluación médica (Doctor) →
  Programación de seguimiento (Atención al público).

**Exámenes asignables:** en Consulta médica, Hemograma completo, Glucosa en ayunas, Examen general
de orina y Rayos X de tórax (Electrocardiograma está asociado pero inactivo y no se ofrece); en
Evaluación médica, Glucosa en ayunas y Rayos X de tórax.

**Consultas** (fechas relativas a hoy, hora de El Salvador; UUID `de120010-…-N`):

| N | Consulta | Paciente | Día y hora | Procedimiento | Estado inicial |
| --- | --- | --- | --- | --- | --- |
| 1 | SA-1 | Ana | hace 20 días, 09:00 | Consulta general | Cerrada; orden de Hemograma completo. |
| 2 | SA-2 | Julia | ayer, 15:00 | Control de hipertensión | Cerrada. |
| 3 | SA-3 | Ana | **hoy, 08:00** | Consulta general | Triaje completado (Lucía); Consulta médica PENDIENTE para Carlos. |
| 4 | SA-4 | Julia | **hoy, 09:15** | Control de hipertensión | Toma de presión PENDIENTE para Lucía. |
| 5 | SA-5 | Juan | **hoy, 10:30** | Consulta general | Triaje PENDIENTE para Lucía. |
| 6 | SA-6 | Ana | **hoy, 11:45** | Consulta general | Triaje y Consulta médica completados, orden de Glucosa en ayunas; Entrega de indicaciones PENDIENTE para Marta. |
| 7 | SA-7 | Juan | dentro de 3 días, 08:30 | Consulta general | Triaje PENDIENTE. |
| 8 | SA-8 | Ana | dentro de 10 días, 10:00 | Control de hipertensión | Toma de presión PENDIENTE. |
| 9 | SS-1 | Ana | hace 10 días, 14:00 | Consulta general | Cerrada (Rosa, Jorge, Marta). |
| 10 | SS-2 | Ana | **hoy, 13:00** | Consulta general | Triaje completado (Rosa); Consulta médica PENDIENTE para Carlos. |
| 11 | SS-3 | Ana | dentro de 5 días, 09:00 | Control de hipertensión | Toma de presión PENDIENTE. |

SA-1 a SA-8 son de Clínica Santa Ana; SS-1 a SS-3, de Clínica San Salvador.

## 11. Errores comunes

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `403` con `"code":"auth.origen"` al iniciar sesión o en cualquier escritura | La petición no trae `Origin` (o `Referer`) aceptado: el frontend no corre en el puerto 3000, o se usa curl sin `-H 'Origin: http://localhost:3000'`. | Levantar ChivoMed en el puerto 3000, o definir `CHIVOMED_ORIGINS` antes de `bash scripts/liberty.sh` (sección 8). En curl, enviar la cabecera `Origin`. |
| `403` `consulta.noResponsablePaso` al completar, retroceder u ordenar | El paso lo atiende otro rol (por ejemplo, Carlos intenta completar el Triaje). | Entrar con un empleado que tenga el rol del paso en esa clínica (tabla de la sección 10). |
| `preparar-bd-desarrollo.sh` falla con `duplicate key value violates unique constraint "uq_documento_tipo_valor"` | Otra persona de la base ya tiene uno de los documentos demo (`0100000N-N`). | Revisar esa persona; si es la demo anterior, seguir la sección 5.1. |
| La limpieza falla con `No se borró nada: hay datos de otras personas que dependen de la demo anterior` | Hay filas que no son de la demo colgando de ella (el mensaje dice cuáles). | Revisarlas y moverlas o borrarlas a mano, o dejar la demo anterior. La base quedó sin cambios. |
| `docker run` con `postgres:18` falla diciendo que hay datos en `/var/lib/postgresql/data` | La imagen 18 cambió la ruta del volumen. | Montar el volumen en `/var/lib/postgresql` (sección 2), o usar `postgres:17` con `/var/lib/postgresql/data`. |
| Las migraciones fallan con `function gen_random_uuid() does not exist` u otra sintaxis | PostgreSQL anterior a 13. | Usar PostgreSQL 17 o 18. |
| La prueba SQL de una FK `RESTRICT` falla en 17 y no en 18 (o al revés) | PostgreSQL 18 informa `restrict_violation` (23001) donde 17 informaba `foreign_key_violation` (23503). | Las pruebas de `docs/migrations/tests` ya aceptan los dos. Actualice el repositorio. |
| `bash scripts/liberty.sh` no arranca: puerto `9080` en uso (`CWWKO0221E`) | Otro Liberty (otra terminal o un `liberty:start` previo) u otro programa. | `ss -ltnp \| grep 9080` o `lsof -i :9080` para verlo; detener el otro Liberty con `mvn liberty:stop` o cerrar el programa. |
| `docker run` falla: `port is already allocated` en `5432` | Hay un PostgreSQL local u otro contenedor usando el puerto. | Publicar otro puerto (`-p 5433:5432`) y poner `PG_PORT=5433` en `.env`. |
| `preparar-bd-desarrollo.sh` termina con `Primero ejecuta: mvn -DskipTests package` | Falta compilar. | Paso 4. |
| `preparar-bd-desarrollo.sh` falla con `No such container` o `role "…" does not exist` | El contenedor o el usuario de PostgreSQL tienen otro nombre. | Pasar los argumentos: `bash scripts/preparar-bd-desarrollo.sh CONTENEDOR BASE USUARIO`. |
| El despliegue indica órdenes con `id_examen` pendientes de conversión | La base propia conserva asociaciones en la columna antigua. | Detener la app, respaldar y seguir [la conversión](conversion-ordenes-base-original.md). La base original + parche no necesita esa columna. |
| `…/examenes-asignables` devuelve `[]` en "Consulta médica" | Faltan los datos demo, o el examen o su tipo están inactivos. | Volver a ejecutar `preparar-bd-desarrollo.sh`. |
| El login con `01000001-1` da 401, o `publico/tipos-documento` devuelve `[]` | No se cargaron los datos demo, o `PG_DB` apunta a otra base. | Revisar `PG_DB` en `.env` y volver a ejecutar `preparar-bd-desarrollo.sh`. |
| `reiniciar-demo.sh` falla con `Faltan los datos demo del guion` | La base no se preparó con el script. | Ejecutar primero `preparar-bd-desarrollo.sh`. |
| El dashboard de hoy no muestra consultas | Las fechas demo se anclaron otro día. | `bash scripts/reiniciar-demo.sh` el día de la revisión (sección 9). |
| Liberty no se conecta a la base (`DSRA…`, `Connection refused` o `password authentication failed`) | El contenedor está detenido, o el usuario o la contraseña de `.env` no coinciden. | `docker start galenasv`; revisar `PG_USER`, `PG_PASS` y `PG_PORT` en `.env` y reiniciar Liberty. |

## Resumen

```bash
docker run -d --name galenasv -e POSTGRES_PASSWORD=CLAVE_LOCAL -p 5432:5432 -v galenasv-datos:/var/lib/postgresql postgres:18
cp src/main/liberty/config/server.xml.ejemplo src/main/liberty/config/server.xml
cp .env.example .env                              # completar valores locales
chmod 600 .env
mvn -DskipTests package
bash scripts/preparar-bd-desarrollo.sh
bash scripts/liberty.sh                                   # en otra terminal, después:
curl -s http://localhost:9080/galeno/api/publico/tipos-documento
bash scripts/reiniciar-demo.sh                    # el día de la revisión
```

### Notas al retroceder un paso

El retroceso conserva observaciones e indicaciones usando `consulta_procedimiento_paso.valor`
del parche original. No requiere una columna `motivo_retroceso` ni ejecutar SQL adicional.
La aplicación sigue leyendo los registros anteriores en texto plano. Ver [API REST](api-rest.md).
