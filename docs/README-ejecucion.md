# Ejecutar GalenoSV desde un clon

## Requisitos

- JDK 21 (`java -version` y `mvn -version` deben mostrar Java 21).
- Maven y PostgreSQL.
- Open Liberty 26.0.0.8 en `/opt/wlp_26.0.0.8` para usar la instalación local indicada abajo. Si se instala en otra ruta, cambie `liberty.installDir`.

## Base de datos nueva

Ejecute estos comandos con un administrador local de PostgreSQL y elija una contraseña propia. El esquema está vacío y ya incluye las dos migraciones de `docs/migrations`.

```bash
sudo -u postgres psql -c "CREATE ROLE galenosv LOGIN PASSWORD 'CAMBIAR_CONTRASENA';"
sudo -u postgres createdb -O galenosv galenosv
PGPASSWORD='CAMBIAR_CONTRASENA' psql -h localhost -U galenosv -d galenosv -v ON_ERROR_STOP=1 -f docs/schema.sql
```

Copie la configuración y sustituya los valores de ejemplo por los de su instalación. Conserve `server.xml` solo en su equipo; Git lo ignora.

```bash
cp src/main/liberty/config/server.xml.example src/main/liberty/config/server.xml
${EDITOR:-vi} src/main/liberty/config/server.xml
```

La configuración usa el driver incluido en `src/main/liberty/config/lib/global`, el recurso JNDI `jdbc/postgres` y el puerto HTTP 9080. La base nueva no contiene registros: cree primero una clínica activa, el rol activo **Doctor** o **Médico**, una persona y su asignación a ese rol y clínica; cree otra persona con rol **Paciente** en la misma clínica. En **Cambiar de rol**, elija la clínica y la persona con rol Doctor/Médico. Esa es la identidad de trabajo para **Consultas**; no hay autenticación de usuarios independiente.

## Iniciar

```bash
export JAVA_HOME=/ruta/a/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
mvn package
mvn -Dliberty.installDir=/opt/wlp_26.0.0.8 liberty:dev
```

Abra [Cambiar de rol](http://localhost:9080/galenosv/paginas/cambiar-rol.xhtml) y después [Consultas](http://localhost:9080/galenosv/paginas/consultas.xhtml). `mvn package` no ejecuta `clean` ni borra el servidor local.
