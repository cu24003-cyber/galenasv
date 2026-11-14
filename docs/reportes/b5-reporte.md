# Reporte B5: exámenes asignables y órdenes con examen (HU4)

Fecha: 2026-11-13. Rama `backend-rubrica`. Sin push.

## Resultado

Quedó implementado lo del plan B5 (opción A en las 4 decisiones). Además se corrigieron dos
problemas encontrados durante la tarea, cada uno en su propio commit:

- la prueba SQL de la FK fallaba según la versión de PostgreSQL;
- en Liberty, las rutas anidadas ignoraban al padre de la URL. Era una fuga de datos entre
  clínicas.

`mvn test`, al final: **731 pruebas, 0 fallos, 0 errores, 0 omitidas**. Antes de la B5 eran 706.

| Commit | Contenido |
| --- | --- |
| `b42563f` | Migración `20261113_orden_examen_examen.sql`, datos demo `20261113_datos_desarrollo_examenes.sql`, sus pruebas SQL y `scripts/preparar-bd-desarrollo.sh`. |
| `5877658` | `GET /api/consultas-procedimientos-pasos/{id}/examenes-asignables`. |
| `80deecf` | La prueba SQL de `ON DELETE RESTRICT` acepta 23503 y 23001. |
| `df9a08a` | Fix: el padre de las colecciones anidadas se fija con una llamada que atraviesa el proxy CDI. |
| `37779de` | `OrdenExamen.idExamen`; el POST anidado de órdenes exige un examen asignable. |
| `f4b41b9` | Documentación en `api-rest.md` y `openapi.json`. |

## Decisiones aplicadas

1. **JSF sin cambios.** `ordenar(consulta, paso, indicaciones)` se conserva para la vista y
   sigue creando órdenes sin examen.
2. **`indicaciones` opcional cuando hay examen**, hasta 255 caracteres, recortado. Se quitó
   `@NotBlank` de `OrdenExamenDto`; se conserva `@Size(max = 255)`.
3. **El paso se toma de la URL.** Por tu decisión del 13/11 se usa el mecanismo que ya existía,
   `relacion.inconsistente`, y no `id.inconsistente`. Antes del fix, ese mecanismo no funcionaba
   en Liberty (ver más abajo).
4. **Un archivo de datos demo de exámenes:**
   - 3 asignables a "Evaluación médica";
   - "Perfil lipídico", con la asociación inactiva;
   - "Examen descontinuado (demo)", con el examen inactivo.

## Prueba SQL que falló en tu contenedor

**No era un problema de orden de limpieza.** La prueba no limpia nada: el `DELETE` del examen es
lo que se prueba, porque debe ser rechazado, y al final el `ROLLBACK` revierte todo. La causa es
la versión de PostgreSQL:

- **Hasta la 17**, una FK `ON DELETE RESTRICT` que rechaza el borrado lanza `foreign_key_violation`
  (23503), con el mensaje "violates foreign key constraint". Es el mensaje que viste.
- **Desde la 18** (commit de PostgreSQL "Fix error code for referential action RESTRICT") lanza
  `restrict_violation` (23001), con el mensaje "violates RESTRICT setting of foreign key
  constraint".

Mi PostgreSQL local es la 18.6, así que ajusté la prueba a 23001 y dejó de funcionar en tu
contenedor, que da el mensaje de la 17 o anterior. La comprobación de la FK (`confdeltype = 'r'`)
pasó en tu base, así que la restricción está bien creada.

Ahora la prueba acepta los dos códigos. La verifiqué completa en PostgreSQL 18; para 23503
verifiqué la cláusula con `RAISE foreign_key_violation`. No tengo una versión 17 para correrla
completa.

## Rutas anidadas: el padre se perdía en Liberty

**Causa.** `AbstractResource.relacionados()` hacía `recurso.padre = …` sobre lo que devuelve
`ResourceContext.getResource()`. En Liberty eso es un proxy CDI del recurso `@RequestScoped`, y
la escritura del campo quedaba en el proxy, no en la instancia que atiende la petición. Por eso
`idPadre()` y `contextoColeccion` llegaban en null.

Las pruebas JerseyTest no lo detectaban, porque ahí no hay CDI.

**Efectos medidos en Liberty antes del fix,** con doctor.demo en la clínica 1:

- `GET /api/consultas/…-2/procedimientos` devolvió **6** procedimientos en vez de 1. Tres eran de
  consultas de la **clínica 2**, que por su ruta directa dan 403.
- `OPTIONS` de un hijo anidado ofrecía `PUT` y `DELETE`.
- El POST anidado de órdenes no pasaba por `AtencionService.ordenar`, sino por el CRUD genérico.
  De ahí venía el 400 de la B4 al omitir `idConsultaProcedimientoPaso`. La orden de la prueba de
  la B4 se creó por esa vía.

**Fix (`df9a08a`).** `relacionados()` llama a `recurso.usarColeccion(padre, relacionada)`, un
método público que no es final, así que el proxy lo delega a la instancia real. La prueba
`ColeccionRelacionadaProxyTest` simula el proxy con un mock de Mockito que delega en la instancia
real, con campos propios. Estaba en rojo con el código anterior.

### Verificación con curl de las rutas anidadas de `api-rest.md`

Solo lecturas, con doctor.demo en la clínica 1, después del fix. Columnas:

- **Solo hijos del padre:** el listado anidado es idéntico al listado de primer nivel filtrado por
  el mismo padre (`/api/<hijo>?<filtro>=<padre>`).
- **Sin filtro:** cuántos registros devuelve el listado de primer nivel sin filtro de padre (el
  filtro de permisos puede igual limitarlo a la clínica); "(403)" significa que DOCTOR no lo
  puede leer sin filtro.
- **OPTIONS:** se compara con la tabla de `api-rest.md`. Los métodos indicados se suman a
  GET, HEAD y OPTIONS.

| Ruta | GET | Solo hijos del padre | Anidado = filtrado; sin filtro | OPTIONS colección | OPTIONS hijo |
| --- | --- | --- | --- | --- | --- |
| `/api/clinicas/{id}/personas` | 200 | sí | 3 = 3; 3 | ok, + POST | ok |
| `/api/clinicas/{id}/personas-roles` | 200 | sí | 4 = 4; 4 | ok, + POST | ok, + PUT, PATCH, DELETE |
| `/api/clinicas/{id}/consultas` | 200 | sí | 3 = 3; 3 | ok | ok |
| `/api/consultas/{id}/procedimientos` | 200 | sí | **1 = 1** (antes del fix: 6); (403) | ok, + POST | ok (antes del fix: + PUT, DELETE) |
| `/api/consultas-procedimientos/{id}/pasos` | 200 | sí | 2 = 2; (403) | ok | ok |
| `/api/consultas-procedimientos-pasos/{id}/ordenes-examen` | 200 | sí | 1 = 1 (paso 2, con la orden de la prueba); (403) | ok, + POST | ok. Un hijo leído desde otro paso da 404 |
| `/api/examenes/{id}/clasificaciones` | 200 | sí | 1 = 1; 5 | ok, + POST | ok, + PUT, PATCH, DELETE |
| `/api/ordenes-examen/{id}/resultados` | 200 | sí | 0 = 0; (403) | ok, + POST | sin resultados en la demo; no verificado |
| `/api/personas/{id}/documentos` | 200 | sí | 1 = 1; (403) | ok, + POST | ok, + PUT, PATCH, DELETE |
| `/api/personas/{id}/medios-contacto` | 200 | sí | 0 = 0; (403) | ok, + POST | sin contactos en la demo; no verificado |
| `/api/personas/{id}/personas-roles` | 200 | sí | 1 = 1; 4 | ok, + POST | ok, + PUT, PATCH, DELETE |
| `/api/personas/{id}/consultas` | 200 | sí | 3 = 3; 3 | ok | ok |
| `/api/personas-roles/{id}/consultas` | 200 | sí | 3 = 3; 3 | ok, + POST | ok, + PUT, PATCH |
| `/api/personas-roles/{id}/pasos-consulta` | **403** `auth.permiso` | — | — | — | — |
| `/api/procedimientos/{id}/pasos` | 200 | sí | 3 = 3; 3 | ok, + POST | ok |
| `/api/procedimientos/{id}/consultas-procedimientos` | **403** `auth.permiso` | — | — | — | — |
| `/api/procedimientos-pasos/{id}/examenes-configurados` | 200 | sí | 5 = 5; 5 | ok, + POST | ok |
| `/api/procedimientos-pasos/{id}/secuencias` | 200 | sí | 1 = 1; 2 | ok | ok |
| `/api/procedimientos-pasos/{id}/pasos-consulta` | **403** `auth.permiso` | — | — | — | — |
| `/api/roles/{id}/personas` | 200 | sí | 1 = 1; 3 | ok | ok |
| `/api/roles/{id}/personas-roles` | 200 | sí | 1 = 1; 4 | ok, + POST | ok, + PUT, PATCH, DELETE |
| `/api/roles/{id}/procedimientos-pasos` | 200 | sí | 3 = 3; 3 | ok | ok |
| `/api/tipos-documento/{id}/documentos` | **403** `auth.permiso` | — | — | — | — |
| `/api/tipos-examen/{id}/examenes` | 200 | sí | 5 = 5; 5 | ok | ok |
| `/api/tipos-examen/{id}/clasificaciones` | 200 | sí | 5 = 5; 5 | ok, + POST | ok, + PUT, PATCH, DELETE |
| `/api/tipos-medio-contacto/{id}/medios-contacto` | **403** `auth.permiso` | — | — | — | — |

Los 5 casos en 403 vienen de `PermisosApiFilter` y ya existían antes; no los cambié:

- **`pasos-consulta`, en las dos rutas:** el filtro no conoce ese segmento, lo trata como recurso
  desconocido y lo deniega a cualquier rol.
- **`procedimientos/{id}/consultas-procedimientos`, `tipos-documento/{id}/documentos` y
  `tipos-medio-contacto/{id}/medios-contacto`:** el padre es un catálogo, así que el filtro exige
  el filtro de dueño (`idConsulta` o `idPersona`). Evita listar datos de otras clínicas.

Hay que decidir si esas 5 rutas se quitan de la documentación o se habilitan. No es parte de la
B5.

## Prueba con curl contra Liberty

Con doctor.demo, consulta `de110009-…-2` (clínica 1):

- paso 1, "Toma de signos vitales": `de11000b-…-21`, COMPLETADO;
- paso 2, "Evaluación médica": `de11000b-…-22`, PENDIENTE.

| # | Petición | Resultado |
| --- | --- | --- |
| 1 | `GET …/{paso 2}/examenes-asignables` | 200: Examen general de orina, Glucosa en ayunas, Hemograma completo. Sin el examen inactivo ni el de la asociación inactiva. |
| 2 | `GET …/{paso 1}/examenes-asignables` | 200 `[]` |
| 3 | La misma lista por la ruta anidada `consultas-procedimientos/…/pasos/{paso 2}/…` | 200, la misma lista. |
| 4 | Orden sin `idExamen` | 400 `consulta.examenObligatorio` |
| 5 | Orden con el examen inactivo (`de11000c-…-5`) | 400 `consulta.examenNoAsignable` |
| 6 | Orden con la asociación inactiva (`…-4`, Perfil lipídico) | 400 `consulta.examenNoAsignable` |
| 7 | Orden con `idConsultaProcedimientoPaso` del paso 1 en el cuerpo | 400 `relacion.inconsistente` |
| 8 | Orden en el paso 1, completado | 409 `consulta.ordenPasoCompletado` |
| 9 | Orden de Hemograma sin `idConsultaProcedimientoPaso` en el cuerpo, `"indicaciones":"  En ayunas  "` | **201**, con `idExamen`, el paso de la URL e `"indicaciones":"En ayunas"` |
| 10 | `GET …/{paso 2}/ordenes-examen` | 200, solo esa orden. |
| 11 | `PUT …/{paso 2}/retroceso`, sin cuerpo | 409 `consulta.retrocesoConRegistros` |
| 12 | ADMINISTRACION clínica 1 (`de110002-…-6`): GET de asignables y POST de orden | 403 y 403 |
| 12 | DOCTOR clínica 2 (`de110002-…-5`): GET de asignables y POST de orden | 403 y 403 |

El log completo, con los cuerpos de cada respuesta, se guardó en el directorio temporal de la
sesión.

**Observación.** Con `Content-Type: application/json` y el cuerpo vacío, las acciones sin cuerpo
(`…/retroceso`, `…/sesion`) responden 400 `json.invalido`, porque el filtro de permisos intenta
leer el JSON. Ya pasaba antes de la B5. Lo documenté en `api-rest.md` ("enviar sin
Content-Type"), pero no cambié el filtro.

**Datos modificados en la demo:** una orden de Hemograma en el paso 2 de la consulta `…-2`.
Corre `scripts/reiniciar-demo.sh` para volver al estado inicial.

## Pendiente o fuera de alcance

- La vista JSF sigue sin selector de examen (decisión 1A).
- Las 5 rutas anidadas documentadas que el filtro deniega (tabla de arriba).
- La respuesta 400 `json.invalido` de las acciones con `Content-Type` y sin cuerpo.
- La prueba SQL no se corrió en PostgreSQL 17. En tu contenedor, vuelve a correr:
  `docker exec -i postgres psql -X -v ON_ERROR_STOP=1 -U raul -d galenasv < docs/migrations/tests/orden_examen_examen_test.sql`
