# Base de datos para el acceso de ChivoMed

## Aplicación de la migración

Ejecutar sobre la base de GalenoSV que ya contiene `persona`, `rol` y `persona_rol`.
Crear previamente un respaldo con `pg_dump`. Se requiere PostgreSQL 13 o posterior
por `gen_random_uuid()`; no se instala ninguna extensión.

Desde la raíz del repositorio, con una conexión PostgreSQL configurada:

```bash
psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261104_usuarios_roles.sql
psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/tests/usuarios_roles_test.sql
```

La migración es transaccional y se puede repetir. No elimina ni renombra roles,
personas, asignaciones o historial. Si hay varios roles existentes que coinciden
con un mismo código, se detiene y revierte para que el equipo elija el rol canónico
sin fusionar asignaciones automáticamente. Conserva el estado de los roles existentes,
incluso si están inactivos. Los roles nuevos se crean activos.

En el entorno local de Julio, el contenedor y la base se llaman `galenasv`:

```bash
docker exec -i galenasv psql -X -v ON_ERROR_STOP=1 -U postgres -d galenasv < docs/migrations/20261104_usuarios_roles.sql
docker exec -i galenasv psql -X -v ON_ERROR_STOP=1 -U postgres -d galenasv < docs/migrations/tests/usuarios_roles_test.sql
```

Cada compañero debe sustituir contenedor, usuario y base según su entorno.
Las pruebas crean datos temporales dentro de una transacción y terminan en `ROLLBACK`.

## Cambios de esquema

Se añade `rol.codigo`, con un índice único y estos valores base:

| Código | Nombre de un rol nuevo | Nombres existentes reconocidos |
| --- | --- | --- |
| `ADMINISTRACION` | Administración | Administración, Administrador/a, Administrativo/a, Secretario/a |
| `DOCTOR` | Doctor | Doctor/a, Médico/a |
| `PACIENTE` | Paciente | Paciente |

Se ignoran mayúsculas, espacios exteriores y acentos al reconocer nombres.
Los códigos se asocian al UUID existente; los nombres conservan su valor original
para no romper las reglas clínicas actuales. Otros roles existentes se conservan.

Se crea `usuario` con:

| Campo | Regla |
| --- | --- |
| `id_usuario` | UUID generado por PostgreSQL si se omite. |
| `id_persona` | Obligatorio, referencia a `persona`; una cuenta por persona. |
| `nombre_usuario` | Obligatorio, único sin distinguir mayúsculas; letras ASCII, números, punto, guion y guion bajo, empezando por letra o número. |
| `correo` | Opcional, único sin distinguir mayúsculas; si se envía debe tener formato básico de correo y no tener espacios. |
| `password_hash` | Obligatorio, formato PHC de Argon2id. |
| `activo` | Obligatorio, `false` por defecto; la activación deberá ser autorizada. |
| `fecha_creacion` | Fecha y hora con zona, generadas por la base. |

No se añade un rol directamente a `usuario`: sus roles y clínicas siguen en
`persona_rol`. La cuenta tampoco convierte automáticamente un paciente en doctor
o administrador. No se crean cuentas, contraseñas por defecto ni asignaciones.
Una persona con cuenta no puede eliminarse sin gestionar antes esa cuenta.

## Siguiente etapa del backend

Esta migración prepara el almacenamiento; no implementa autenticación ni permisos.
Falta mapear `Usuario` y `Rol.codigo` en Java, generar y verificar hashes Argon2id
con parámetros seguros, definir quién crea/activa cuentas y asigna roles, e implementar
login, sesión actual y logout. La comprobación SQL del hash verifica su formato,
no demuestra que se haya calculado criptográficamente ni valida su coste.

El backend tendrá que resolver usuario o correo sin distinguir mayúsculas, validar
la cuenta activa y comprobar que el rol/clínica elegido pertenece a la persona
autenticada. El nombre de usuario no permite `@`, evitando ambigüedad con correos.
La validez y verificación del correo se completarán en la aplicación.

Se mantienen pendientes los acuerdos de permisos entre Administración, Doctor y
Paciente. No se modifica la configuración local de Liberty ni el frontend.
