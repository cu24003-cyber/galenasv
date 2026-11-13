# Levantar el backend de GalenoSV desde cero

Guía para dejar el backend (`galenasv`) funcionando en otra computadora para la revisión:

- Open Liberty en `http://localhost:9080/galenosv`, con la API en `/galenosv/api`;
- PostgreSQL en Docker, con los datos demo de las HU1 a HU4.

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

## 3. Configurar Liberty (`server.xml`)

`src/main/liberty/config/server.xml` está en `.gitignore` porque lleva credenciales. Se crea a
partir de la plantilla versionada, que no tiene contraseñas reales:

```bash
cp src/main/liberty/config/server.xml.ejemplo src/main/liberty/config/server.xml
```

Editar `src/main/liberty/config/server.xml` y reemplazar:

| Atributo | Valor |
| --- | --- |
| `user` | `postgres`, o el usuario del contenedor |
| `password` | La `CLAVE_LOCAL` del paso 2 |
| `keysPassword` (de `<ltpa>`) | Cualquier texto propio |
| `databaseName` | `galenasv`, o el nombre de base que se pase al script del paso 5 |
| `portNumber` | `5432`, o el puerto publicado en `docker run -p` |

No suba `server.xml` a Git.

## 4. Compilar

```bash
mvn -DskipTests package
```

El script del paso 5 lo exige: comprueba que existan
`target/classes/.../ContrasenaService.class` y `target/galenosv/WEB-INF/lib/password4j-1.8.4.jar`.
Si faltan, se detiene con `Primero ejecuta: mvn -DskipTests package`.

Para correr las pruebas: `mvn test`. No necesita la base ni Liberty.

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
4. aplica `20261113_orden_examen_examen.sql`, la columna `orden_examen.id_examen` de la HU4;
5. carga `20261111_datos_desarrollo.sql`: clínica, cuentas demo y DUI. Genera un hash aleatorio
   con `ContrasenaService`, porque el login es por documento y no usa contraseña;
6. carga `20261112_datos_desarrollo_clinicos.sql`: segunda clínica, procedimiento y siete consultas;
7. carga `20261113_datos_desarrollo_examenes.sql`: catálogo de exámenes asignables.

Debe terminar con `Base preparada: galenasv. …`. **Se puede repetir sin error**: se verificó la
secuencia completa dos veces sobre una base existente. No borra datos.

## 6. Arrancar Liberty

```bash
mvn liberty:run
```

Deja el servidor en primer plano; se detiene con `Ctrl+C`. La primera vez descarga Open Liberty
y la feature `jakartaee-11.0`, así que tarda varios minutos. Al recompilar (`mvn compile`,
`mvn test` o `mvn package`), Liberty recarga la aplicación solo: aparece `CWWKZ0003I` en el log.

## 7. Verificar que está arriba

1. **Log de Liberty.** En la consola o en
   `target/liberty/wlp/usr/servers/galenosv/logs/messages.log` deben aparecer:
   ```text
   CWWKT0016I: Web application available (default_host): http://localhost:9080/galenosv/
   CWWKF0011I: The galenosv server is ready to run a smarter planet. ...
   ```
   No debe haber líneas `E ` (error) de JPA o del dataSource.
2. **API pública, sin sesión:**
   ```bash
   curl -s http://localhost:9080/galenosv/api/publico/tipos-documento
   # [{"expresionRegular":…,"idTipoDocumento":"de110004-0000-4000-8000-000000000001","indicaciones":…,"nombre":"DUI"}]
   ```
   El JSON debe incluir el DUI demo; si responde `[]`, faltan los datos del paso 5.
3. **Login de un empleado.** Responde `204` y crea la sesión; la cabecera `Origin` es
   obligatoria, ver "Errores comunes":
   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -c /tmp/galenosv.cookies \
     -H 'Origin: http://localhost:3000' -H 'Content-Type: application/json' \
     -X POST http://localhost:9080/galenosv/api/auth/login \
     -d '{"idTipoDocumento":"de110004-0000-4000-8000-000000000001","numeroDocumento":"00000000-2"}'
   # 204
   curl -s -b /tmp/galenosv.cookies http://localhost:9080/galenosv/api/atencion/dashboard
   # [ … consultas de hoy de la clínica 1 … ]
   ```

`GET /api/jakartaee11` **no** sirve para comprobar el servidor: exige sesión y sin ella responde
401.

## 8. Frontend (ChivoMed) en el puerto 3000

El frontend se levanta en su repositorio con `npm start`, que sirve en `http://localhost:3000`
y reenvía `/galenosv/api` a `http://localhost:9080/galenosv/api`. **Debe correr en el puerto
3000**:

- el backend solo acepta escrituras (login, cambio de rol, acciones) cuyo `Origin` sea el propio
  backend o uno de la variable de entorno `CHIVOMED_ORIGINS`;
- sin esa variable, se aceptan `http://127.0.0.1:3000` y `http://localhost:3000`.

Para usar otro puerto u otro host, definir la variable **antes** de arrancar Liberty, en la misma
terminal. Reemplaza los valores por defecto, así que incluya también los que quiera conservar:

```bash
CHIVOMED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000,http://localhost:3001 mvn liberty:run
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

- **Qué hace:** vuelve a anclar las siete consultas demo al día de hoy y les devuelve su estado
  inicial. Borra las órdenes, resultados, pasos y procedimientos agregados desde la app **solo**
  a esas consultas.
- **Qué no toca:** ningún otro dato.
- **No hace falta reiniciar Liberty.**

## 10. Datos demo

Todos los documentos son de tipo **DUI** (`de110004-0000-4000-8000-000000000001`). Los datos son
ficticios.

| DUI | Cuenta | Asignaciones (rol y clínica) | Login |
| --- | --- | --- | --- |
| `00000000-1` | admin.demo | ADMINISTRACION en Clínica de demostración | Entra (HU2). |
| `00000000-2` | doctor.demo | DOCTOR en Clínica de demostración (**rol inicial**); DOCTOR en Clínica de demostración Norte; ADMINISTRACION en Clínica de demostración | Entra y puede cambiar de rol y clínica (HU3). |
| `00000000-3` | paciente.demo | PACIENTE en las dos clínicas | **Rechazado** (401): solo los empleados se autentican (HU2). |

Las siete consultas son de paciente.demo y todas usan el procedimiento demo de 3 pasos; las fechas se calculan al cargar o al reiniciar:

| Consulta | Clínica | Fecha | Estado inicial |
| --- | --- | --- | --- |
| `de110009-…-1` | Demostración | hace 20 días, 09:00 | Cerrada, los 3 pasos completados. |
| `de110009-…-2` | Demostración | **hoy, 08:00** | Abierta: paso 1 completado, paso 2 "Evaluación médica" pendiente (HU4). |
| `de110009-…-7` | Demostración | **hoy, 10:30** | Nueva: paso 1 "Toma de signos vitales" PENDIENTE, sin valor. Sirve para atender una consulta desde el inicio. |
| `de110009-…-3` | Demostración | dentro de 7 días, 10:00 | Nueva: paso 1 pendiente. |
| `de110009-…-4` | Norte | hace 12 días, 14:00 | Cerrada. |
| `de110009-…-5` | Norte | **hoy, 11:00** | Nueva. |
| `de110009-…-6` | Norte | dentro de 3 días, 15:30 | Nueva. |

**Procedimiento demo:** "Toma de signos vitales" → "Evaluación médica" → "Indicaciones y
cierre".

**Exámenes asignables a "Evaluación médica":** Hemograma completo, Glucosa en ayunas y Examen
general de orina. También hay "Perfil lipídico", con la asociación inactiva, y "Examen
descontinuado (demo)", inactivo; ninguno de los dos se ofrece.

## 11. Errores comunes

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `403` con `"code":"auth.origen"` al iniciar sesión o en cualquier escritura | La petición no trae `Origin` (o `Referer`) aceptado: el frontend no corre en el puerto 3000, o se usa curl sin `-H 'Origin: http://localhost:3000'`. | Levantar ChivoMed en el puerto 3000, o definir `CHIVOMED_ORIGINS` antes de `mvn liberty:run` (sección 8). En curl, enviar la cabecera `Origin`. |
| `401` `auth.credenciales` con `00000000-3` | Es el paciente: la HU2 solo admite empleados. | Usar `00000000-1` o `00000000-2`. |
| `docker run` con `postgres:18` falla diciendo que hay datos en `/var/lib/postgresql/data` | La imagen 18 cambió la ruta del volumen. | Montar el volumen en `/var/lib/postgresql` (sección 2), o usar `postgres:17` con `/var/lib/postgresql/data`. |
| Las migraciones fallan con `function gen_random_uuid() does not exist` u otra sintaxis | PostgreSQL anterior a 13. | Usar PostgreSQL 17 o 18. |
| La prueba SQL de una FK `RESTRICT` falla en 17 y no en 18 (o al revés) | PostgreSQL 18 informa `restrict_violation` (23001) donde 17 informaba `foreign_key_violation` (23503). | Las pruebas de `docs/migrations/tests` ya aceptan los dos. Actualice el repositorio. |
| `mvn liberty:run` no arranca: puerto `9080` en uso (`CWWKO0221E`) | Otro Liberty (otra terminal o un `liberty:start` previo) u otro programa. | `ss -ltnp \| grep 9080` o `lsof -i :9080` para verlo; detener el otro Liberty con `mvn liberty:stop` o cerrar el programa. |
| `docker run` falla: `port is already allocated` en `5432` | Hay un PostgreSQL local u otro contenedor usando el puerto. | Publicar otro puerto (`-p 5433:5432`) y poner `portNumber="5433"` en `server.xml`. |
| `preparar-bd-desarrollo.sh` termina con `Primero ejecuta: mvn -DskipTests package` | Falta compilar. | Paso 4. |
| `preparar-bd-desarrollo.sh` falla con `No such container` o `role "…" does not exist` | El contenedor o el usuario de PostgreSQL tienen otro nombre. | Pasar los argumentos: `bash scripts/preparar-bd-desarrollo.sh CONTENEDOR BASE USUARIO`. |
| Error 500 al leer o crear órdenes; en `messages.log` aparece `column … id_examen does not exist` | Falta la migración `20261113_orden_examen_examen.sql`, por ejemplo en una base preparada antes de la HU4. | Volver a ejecutar `bash scripts/preparar-bd-desarrollo.sh …` (se puede repetir) y recompilar o reiniciar Liberty. |
| `…/examenes-asignables` devuelve `[]` en "Evaluación médica" | Faltan los datos demo de exámenes. | Volver a ejecutar `preparar-bd-desarrollo.sh`. |
| El login con `00000000-2` da 401, o `publico/tipos-documento` devuelve `[]` | No se cargaron los datos demo, o `server.xml` apunta a otra base (`databaseName`). | Revisar `databaseName` y volver a ejecutar `preparar-bd-desarrollo.sh`. |
| `reiniciar-demo.sh` falla con `Faltan las cuentas demo` | La base no se preparó con el script. | Ejecutar primero `preparar-bd-desarrollo.sh`. |
| El dashboard de hoy no muestra consultas | Las fechas demo se anclaron otro día. | `bash scripts/reiniciar-demo.sh` el día de la revisión (sección 9). |
| Liberty no se conecta a la base (`DSRA…`, `Connection refused` o `password authentication failed`) | El contenedor está detenido, o el usuario o la contraseña de `server.xml` no coinciden. | `docker start galenasv`; revisar `user`, `password` y `portNumber` en `server.xml` y reiniciar Liberty. |

## Resumen

```bash
docker run -d --name galenasv -e POSTGRES_PASSWORD=CLAVE_LOCAL -p 5432:5432 -v galenasv-datos:/var/lib/postgresql postgres:18
cp src/main/liberty/config/server.xml.ejemplo src/main/liberty/config/server.xml   # y editar credenciales
mvn -DskipTests package
bash scripts/preparar-bd-desarrollo.sh
mvn liberty:run                                   # en otra terminal, después:
curl -s http://localhost:9080/galenosv/api/publico/tipos-documento
bash scripts/reiniciar-demo.sh                    # el día de la revisión
```
