# Acceso de ChivoMed

Requiere la migración `docs/migrations/20261104_usuarios_roles.sql` y una cuenta
activa en `usuario`, vinculada a una persona con un rol y una clínica activos.
No se crean cuentas ni contraseñas predeterminadas al iniciar el servidor.

## Ejecución local

```bash
mvn verify
mvn liberty:install-feature
mvn liberty:run
```

La configuración local de Liberty debe apuntar a PostgreSQL. `server.xml` contiene
credenciales locales y está excluido de Git. La base de Julio en Docker se llama
`galenasv`; cada compañero debe utilizar los datos de su entorno.

En ChivoMed, ejecutar `npm start` o Run de WebStorm. BrowserSync sirve el frontend
en `http://127.0.0.1:3000` y envía `/galenosv/api` al backend en el puerto 9080.
El navegador conserva la cookie; no es necesario habilitar CORS para este recorrido.

Las escrituras de autenticación y cambio de rol comprueban `Origin` o `Referer`.
Se acepta el origen propio de GalenoSV y, para desarrollo, `http://127.0.0.1:3000`
y `http://localhost:3000`. Para otros orígenes de confianza, establecer la variable
`CHIVOMED_ORIGINS` con una lista separada por comas antes de iniciar Liberty.
No usar comodines. La configuración de ejemplo usa cookie `HttpOnly`, `SameSite=Lax`
y ruta del contexto. En producción se requiere HTTPS y `cookieSecure=true`.

## Contrato HTTP

| Operación | Entrada | Resultado |
| --- | --- | --- |
| `POST /api/auth/login` | JSON con `identificador` y `password` | 204 y cookie de sesión nueva. |
| `GET /api/auth/me` | Cookie de sesión | Usuario, persona, rol, clínica y token CSRF. |
| `POST /api/auth/logout` | Cookie y cabecera `X-CSRF-Token` | 204; invalida toda la sesión. |
| `PUT /api/atencion/rol-activo` | Cookie, token CSRF y `idPersonaRol` | Solo admite asignaciones propias y renueva el identificador de sesión. |

El usuario/correo se resuelve sin distinguir mayúsculas. La contraseña conserva
sus espacios. Un usuario inexistente, una contraseña incorrecta y una cuenta
deshabilitada devuelven el mismo 401. Una cuenta sin asignación activa devuelve 403.
La respuesta y el estado del acceso usan `Cache-Control: no-store`.

`/auth/me` devuelve exactamente el contrato utilizado por el frontend:

```text
usuario: { idUsuario, idPersona, nombres, apellidos }
rolActivo: { idPersonaRol, codigo, idClinica, nombreClinica }
csrfToken: token aleatorio asociado a la sesión
```

No devuelve hashes ni contraseñas. Cada consulta de sesión comprueba de nuevo
la cuenta y la asignación en PostgreSQL. Si fueron deshabilitadas o revocadas,
invalida la sesión. Se conservan los límites del flujo clínico al cambiar de rol.
Cuando una persona tiene varias asignaciones, el login usa la más antigua; el
selector de roles del frontend queda para la etapa de navegación por módulos.

La sesión expira tras 30 minutos de inactividad o un máximo de ocho horas.
El límite inicial de acceso es de diez solicitudes de login por dirección en cinco
minutos; devuelve 429 y `Retry-After: 300`. Se mantiene en memoria por instancia y
no confía en cabeceras de IP enviadas por el cliente. Con BrowserSync, las conexiones
al backend comparten la dirección del proxy.

## Hashes y alta de cuentas

Se usa Password4j 1.8.4 con Argon2id v19, 19 MiB, dos iteraciones, un hilo y una sal
aleatoria de 16 bytes. Los parámetros de los hashes se validan antes de reservar
memoria. El alta de cuentas debe hacerse con un responsable autorizado; no se
expone un endpoint público que permita asignarse roles.

Para obtener un hash desde una terminal local, después de `mvn package`:

```bash
java -cp 'target/classes:target/galenosv/WEB-INF/lib/*' sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ContrasenaService
```

La herramienta pide la contraseña sin mostrarla y emite únicamente su hash.
Insertar ese hash en `usuario.password_hash`, vincular `id_persona` y activar la
cuenta solo después de verificar su asignación en `persona_rol`. No enviar
contraseñas como argumentos de consola ni guardarlas en scripts o archivos versionados.

## Alcance de esta etapa

Esta implementación protege las rutas de autenticación y el contexto de atención.
Los demás recursos CRUD y las páginas JSF conservan los controles que tenían:
la matriz completa de permisos y el acceso por clínica/paciente siguen pendientes.
No usar datos clínicos reales hasta completar esa autorización. El frontend actual
abre la pantalla de cuenta; los paneles de Administración, Doctor y Paciente se
integrarán después. No se modificó el esquema clínico ni se borraron registros.

## Resumen de archivos

Los archivos Java de implementación están bajo
`src/main/java/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv/`.

| Estado | Archivo | Cambio |
| --- | --- | --- |
| Nuevo | `entity/Usuario.java` | Mapea la cuenta y evita serializar su hash. |
| Modificado | `entity/Rol.java` | Mapea el código estable del rol. |
| Nuevo | `entity/repository/UsuarioRepository.java` | Busca cuentas y asignaciones propias activas. |
| Nuevo | `control/service/AutenticacionService.java` | Valida credenciales, cuenta y contexto autorizado. |
| Nuevo | `control/service/ContrasenaService.java` | Genera y verifica Argon2id; incluye herramienta local de hashes. |
| Nuevo | `control/service/AccesoSesion.java` | Identidad, token CSRF y duración máxima de la sesión. |
| Nuevo | `control/service/AccesoException.java` | Errores seguros de autenticación y autorización. |
| Nuevo | `control/service/IntentosAcceso.java` | Límite de solicitudes de login por dirección. |
| Modificado | `control/service/ContextoRolService.java` | Lista únicamente las asignaciones de la cuenta actual. |
| Nuevo | `boundary/resources/AuthResource.java` | Login, sesión actual y logout. |
| Nuevo | `boundary/web/AutenticacionFilter.java` | Cookie, origen, CSRF y cabeceras de autenticación. |
| Modificado | `boundary/resources/AtencionResource.java` | Revalida el contexto y rechaza cambios a roles ajenos. |
| Modificado | `boundary/resources/provider/ApiExceptionMapper.java` | Conserva códigos y mensajes seguros de acceso. |
| Modificado | `pom.xml` | Dependencia Password4j. |
| Modificado | `src/main/resources/META-INF/persistence.xml` | Registra Usuario en JPA. |
| Modificado | `src/main/webapp/WEB-INF/web.xml` | Cookie HttpOnly y seguimiento de sesión solo por cookie. |
| Modificado | `src/main/liberty/config/server.xml.example` | Ejemplo de SameSite y ruta de cookie. |
| Modificado | `src/main/webapp/openapi.json` | Documenta el contrato de autenticación y CSRF. |
| Modificado | `src/test/.../boundary/resources/RestApiTest.java` | Adapta el contexto autenticado y comprueba que el código del rol no sea editable. |
| Nuevo | `src/test/.../boundary/resources/AuthResourceTest.java` | Contrato de sesión, renovación e invalidación. |
| Nuevo | `src/test/.../boundary/web/AutenticacionFilterTest.java` | Origen, cookies, expiración y CSRF. |
| Nuevo | `src/test/.../control/service/AccesoSesionTest.java` | Tokens independientes y vencimiento absoluto. |
| Nuevo | `src/test/.../control/service/AutenticacionServiceTest.java` | Credenciales, cuenta deshabilitada y roles ajenos. |
| Nuevo | `src/test/.../control/service/ContextoRolServiceTest.java` | Listado propio y rechazo de asignaciones ajenas. |
| Nuevo | `src/test/.../control/service/ContrasenaServiceTest.java` | Hashes reales, sal aleatoria y parámetros corruptos. |
| Nuevo | `src/test/.../control/service/IntentosAccesoTest.java` | Límite y renovación de la ventana de intentos. |
| Nuevo | `src/test/.../entity/repository/UsuarioRepositoryTest.java` | Normalización de usuario/correo como parámetro. |
| Nuevo | `docs/autenticacion.md` | Instrucciones, contrato y alcance de esta etapa. |

La configuración `src/main/liberty/config/server.xml` y las credenciales
`.env.login-local` son exclusivas del entorno local y están excluidas de Git.
La cuenta de Julio y la clínica de desarrollo están únicamente en su PostgreSQL;
no se añaden como datos predeterminados para los demás entornos.

Para detener el servidor local: `mvn liberty:stop`.
