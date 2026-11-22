# Contenedor backend (Tercer Parcial PPI115)

Imagen `debian:stable` + Java 21 + Open Liberty (feature `webProfile-11.0`). El WAR **no** va
dentro de la imagen: se monta al ejecutarla en `dropins/galeno.war`, igual que en el comando del
profesor.

Al arrancar, `entrypoint.sh` **copia** ese WAR a `apps/galeno.war`, le agrega PrimeFaces y
PrimeFaces Themes (`/home/usuario/libs-app`) en `WEB-INF/lib` y ejecuta `server run galeno`.
Liberty despliega esa copia en `/galeno` (dropins desactivado). El archivo montado no se modifica.
Hace falta porque PrimeFaces 16 registra sus componentes con `@FacesComponent`, y Liberty no
busca anotaciones en la librería global. OpenPDF (librería global) y el driver PostgreSQL no
necesitan esto. Para montar el WAR en otra ruta: `-e WAR_PATH=/ruta/galeno.war`.

| Archivo | Para qué |
| --- | --- |
| `Dockerfile` | Construye la imagen |
| `server.xml` | Configuración de Liberty en el contenedor (`webProfile-11.0`, puerto 9080 en `*`, dropins desactivado, app `apps/galeno.war` en `/galeno`, dataSource `jdbc/postgres` con `PG_*`) |
| `entrypoint.sh` | CMD de la imagen: prepara la copia del WAR con PrimeFaces y arranca Liberty |
| `prueba-bd/01-original.sql` | Esquema original del ingeniero, sin datos |
| `prueba-bd/02-parche.sql` | Parche del ingeniero (`consulta_procedimiento_paso`) |

## 1. Compilar el WAR y construir la imagen

Desde la raíz del repo (`TUUSUARIO` = cuenta de Docker Hub del grupo):

```bash
mvn clean package
unzip -l target/galeno.war | grep WEB-INF/lib    # no debe mostrar nada
docker build -t TUUSUARIO/galeno-backend:1.0 docker/
```

Para fijar la versión de Open Liberty: `--build-arg OL_VERSION=x.y.z.w`. El build imprime la
versión usada (`Open Liberty ...`); anótenla en la bitácora.

## 2. Base de prueba igual a la del ingeniero (vacía)

La base de prueba puede usar la imagen oficial `postgres`, porque no se entrega.

```bash
docker network create ppi115net
docker run -d --name db.penya.org -h db.penya.org --network ppi115net \
  -e POSTGRES_USER=chepe -e POSTGRES_PASSWORD=passchepe -e POSTGRES_DB=galeno \
  -v "$PWD/docker/prueba-bd":/docker-entrypoint-initdb.d:ro \
  postgres:16
```

## 3. Ejecutar como el profesor

Su comando, con `--` normales y sin `--add-host` (aquí la base está en la misma red):

```bash
docker run --rm --name galeno.backend -h galeno.backend -p 9080:9080 \
  -e PG_USER=chepe -e PG_PASS=passchepe -e PG_HOST=db.penya.org \
  -e PG_DB=galeno -e PG_PORT=5432 \
  -v "$PWD/target/galeno.war":/home/usuario/wlp/usr/servers/galeno/dropins/galeno.war \
  --network ppi115net TUUSUARIO/galeno-backend:1.0
```

En el log deben aparecer `WAR preparado: …/dropins/galeno.war -> …/apps/galeno.war (+ primefaces-16.0.0.jar primefaces-themes-16.0.0.jar)`,
`CWWKZ0001I: Application galeno started` (unos 15 s) y `CWWKF0011I`.

## 4. Verificar

```bash
curl -i http://localhost:9080/galeno/api/publico/tipos-documento     # 200 y []
```

Abrir también una pantalla JSF, por ejemplo `http://localhost:9080/galeno/paginas/clinica.xhtml`,
y crear una clínica y un rol con sus botones (prueba PrimeFaces). Generar además un ticket PDF
(prueba OpenPDF). Al reiniciar el contenedor (`docker restart galeno.backend`) la copia se
vuelve a preparar desde el WAR montado.

| Síntoma | Causa probable |
| --- | --- |
| `ERROR: no se encontró el WAR en …/dropins/galeno.war` y el contenedor termina | No se montó el WAR (falta el `-v`) o la ruta del host no existe |
| `ClassNotFoundException` / `NoClassDefFoundError` | Falta un JAR en `lib/global` o en `libs-app` (agregarlo al Dockerfile) |
| Error con `jdbc/postgres` | Variables `PG_*` mal escritas o la base no está en `ppi115net` |
| `column ... does not exist` | La base no es la original + parche |
| `curl` desde el host no responde | Falta `host="*"` en `server.xml` |
| `WELD-001524` … `module java.base does not "opens java.lang"` | Falta `JAVA_HOME`: sin él, Liberty no carga `java9.options` (el JRE headless no trae `include/`) |
| `Undefined component type org.primefaces.component.…` | PrimeFaces quedó en la librería global en vez de en `WEB-INF/lib`: revisar que el log muestre `WAR preparado` y que el CMD sea `entrypoint.sh` |
| `CWWKS9660E` (orb / user registry) y `CWWKZ0022W` | Se volvió a usar `jakartaee-11.0`: activa el ORB, que sin user registry retrasa ~30 s el arranque. Usar `webProfile-11.0` |
| `CWWJP9991W: [eclipselink.server] Failed to find MBean Server` | Aviso inofensivo con `webProfile-11.0`: EclipseLink no registra sus MBeans de JMX |

## 5. Publicar

```bash
docker login
docker push TUUSUARIO/galeno-backend:1.0
```
