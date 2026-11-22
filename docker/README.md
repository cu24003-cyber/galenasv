# Contenedor backend (Tercer Parcial PPI115)

Imagen `debian:stable` + Java 21 + Open Liberty (Jakarta EE 11). El WAR **no** va dentro de la
imagen: se monta al ejecutarla, igual que en el comando del profesor.

| Archivo | Para qué |
| --- | --- |
| `Dockerfile` | Construye la imagen |
| `server.xml` | Configuración de Liberty en el contenedor (puerto 9080 en `*`, dataSource `jdbc/postgres` con `PG_*`) |
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

En el log deben aparecer `CWWKZ0001I: Application galeno started` y `CWWKF0011I`.

## 4. Verificar

```bash
curl -i http://localhost:9080/galeno/api/publico/tipos-documento     # 200 y []
```

Abrir también una pantalla JSF en `http://localhost:9080/galeno/` (prueba que PrimeFaces carga
desde la librería global de Liberty) y generar un ticket PDF (prueba OpenPDF).

| Síntoma | Causa probable |
| --- | --- |
| `ClassNotFoundException` / `NoClassDefFoundError` | Falta un JAR en `lib/global` (agregarlo al Dockerfile) |
| Error con `jdbc/postgres` | Variables `PG_*` mal escritas o la base no está en `ppi115net` |
| `column ... does not exist` | La base no es la original + parche |
| `curl` desde el host no responde | Falta `host="*"` en `server.xml` |

## 5. Publicar

```bash
docker login
docker push TUUSUARIO/galeno-backend:1.0
```
