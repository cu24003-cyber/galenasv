# Plan B4: retroceso de un paso (HU4) y brechas REST de la HU4

Fecha: 2026-11-13. Rama: `backend-rubrica`. Estado: **esperando aprobación**. No se ha
cambiado código.

## Fuente del contrato

`docs/reportes/tarea3-pendiente.md` **no existe**: no está en el repositorio, en el historial de
git ni en `../ChivoMed`. El contrato acordado está en `../ChivoMed/docs/plan.md`:

- línea 42: "HU4 Consulta: … Falta retroceder al paso anterior";
- línea 60: "reabre el paso anterior en el backend con
  `PUT /api/consultas-procedimientos-pasos/{id}/retroceso`. Responde 409 si es el primer paso o
  si el paso actual tiene valor u órdenes; la vista debe mostrar el motivo del 409".

Coincide con lo que pediste. Uso esa línea más tu mensaje. Si `tarea3-pendiente.md` tenía algo
más, pásamelo.

## Lo que hay hoy

- **Acciones de paso** en `ConsultaProcedimientoPasoResource`: `PUT {id}/responsable`,
  `PUT {id}/finalizacion` y `POST {id}/examenes`. Cada una llama a un método de
  `AtencionService` y responde con el DTO del paso. También funcionan en la ruta anidada
  `/consultas-procedimientos/{cp}/pasos/{id}/…`.
- **`AtencionService.completar`:**
  - llama a `abierta(consulta)`, que bloquea la consulta con `PESSIMISTIC_WRITE`, comprueba la
    clínica con `medicoActivo()` y da 409 `consulta.yaCerrada` si está cerrada;
  - valida el paso con `pasoPropio`;
  - lo marca COMPLETADO;
  - crea los sucesores por `ProcedimientoPasoSecuencia` cuando todos sus requisitos están
    completos. No filtra por `tipoSecuencia`.
- **`PermisosApiFilter`** reconoce como acción el último segmento cuando es `cierre`, `sesion`,
  `responsable`, `finalizacion` o `examenes` sobre `consultas` o `consultas-procedimientos-pasos`.
  Sin eso, el PUT a un recurso clínico se deniega. Para `consultas-procedimientos-pasos`
  (CLINICOS) también:
  - exige sesión y una asignación vigente (`autenticacion.contexto`);
  - solo deja escribir a DOCTOR;
  - niega todo a ADMINISTRACION;
  - comprueba que el paso sea de una consulta de la clínica activa (`verificar`).
- **Base de datos:** la única FK hacia `consulta_procedimiento_paso` es la de `orden_examen`
  (`ON DELETE RESTRICT`). Un paso sin órdenes se puede borrar.
- `tipo_secuencia` siempre se guarda como `'SIGUIENTE'`, tanto en `ConfiguracionProcedimientoService`
  como en los datos demo.

## Retroceso: diseño

```
PUT /api/consultas-procedimientos-pasos/{id}/retroceso          (sin cuerpo; cookie + X-CSRF-Token)
PUT /api/consultas-procedimientos/{cp}/pasos/{id}/retroceso      (ruta anidada, mismo efecto)
200 → DTO del paso anterior ya reabierto (como las otras acciones, que devuelven el paso)
```

`AtencionService.retroceder(idConsulta, idPaso)`, en la transacción del EJB, en este orden:

| # | Comprobación | Si falla |
| --- | --- | --- |
| 1 | `abierta(consulta)`: bloqueo, clínica del rol activo, rol médico y consulta abierta | 409 `consulta.yaCerrada`; 400 `consulta.otraClinica` o `consulta.soloMedico` (existentes) |
| 2 | `pasoPropio(consulta, id)` | 400 `consulta.pasoPropio` (existente) |
| 3 | El paso está PENDIENTE (`fechaFin == null`) | 409 `consulta.pasoYaCompletado` (existente, ya en `CONFLICT`) |
| 4 | El paso tiene definición de catálogo | 400 `consulta.pasoSinDefinicionHistorica` (existente) |
| 5 | Predecesores de la definición: `ProcedimientoPasoSecuencia` con `idProcedimientoPasoReferencia = definición` y `tipoSecuencia = 'SIGUIENTE'` | Ninguno: **409 `consulta.retrocesoPrimerPaso`**. Más de uno: 409 `consulta.retrocesoRamificado` (decisión 2) |
| 6 | El paso no tiene valor (`valor` null o en blanco) ni órdenes de examen (`COUNT(OrdenExamen)`) | **409 `consulta.retrocesoConRegistros`** |
| 7 | El paso anterior se ejecutó en el mismo `ConsultaProcedimiento` y está COMPLETADO (si hubiera varias ejecuciones, la de `fechaFin` más reciente) | 409 `consulta.retrocesoAnteriorPendiente` |
| 8 | Del anterior no salió otro sucesor ya creado en ese procedimiento, aparte del paso actual | 409 `consulta.retrocesoRamificado` (decisión 2) |
| 9 | `em.remove(paso)`; el anterior queda en `estado = PENDIENTE` y `fechaFin = null`, **conservando `valor`** y su responsable | — |

- **Todo o nada:** si cualquier comprobación falla no se cambia nada. El bloqueo de la consulta
  serializa el retroceso con `completar` y las demás acciones sobre la misma consulta.
- **Los 4 códigos nuevos** son de conflicto de estado: van a `CONFLICT` de `ApiExceptionMapper`
  (409), como `consulta.cerrarAntesCrear`. Solo los usa REST (la vista JSF no tiene retroceso),
  así que no van al bundle i18n, igual que `citas.rango` y los `dashboard.*`.
- **Ruta:** `@PUT @Path("{id}/retroceso")` en `ConsultaProcedimientoPasoResource`, igual que
  `finalizacion`: `buscarExistente`, luego `atencion.retroceder(consultaDe(paso), id)`, y
  responde `toDto(anterior)`.
- **`PermisosApiFilter`:** agregar `"retroceso"` al `Set.of("cierre", "sesion", "responsable",
  "finalizacion", "examenes")`. Con eso entra como acción del recurso
  `consultas-procedimientos-pasos`, y el filtro aplica sus reglas: sesión, asignación vigente,
  clínica del paso igual a la activa y escritura solo para DOCTOR.

### Efecto conocido al volver a avanzar

El anterior queda PENDIENTE con su `valor`. Si después se completa con
`PUT …/finalizacion` y `valor` null o vacío, `completar` **borra** ese valor, porque hoy lo
reemplaza siempre. La vista JSF no tiene ese problema porque reenvía el valor del campo. La SPA
debe precargar el valor y reenviarlo. No propongo cambiar `completar`, porque cambiaría el
contrato de `finalizacion`; lo documento en `api-rest.md`.

Al volver a completar el anterior, `completar` recrea el paso borrado con un responsable
automático. El responsable que se hubiera reasignado a mano en el paso borrado se pierde, cosa
aceptable porque ese paso estaba vacío.

## Decisiones que necesito de ti

1. **¿Quién puede retroceder?** Pediste "solo empleados de la clínica del rol activo".
   - **(A, recomendada)** Solo DOCTOR, como `finalizacion`, `responsable` y las demás acciones
     de paso. Lo dan `PermisosApiFilter`, que ya niega a ADMINISTRACION todos los recursos
     clínicos, y `abierta()` → `medicoActivo()`. No hay cambios de permisos.
   - (B) DOCTOR y ADMINISTRACION. Habría que abrir a ADMINISTRACION la escritura en
     `consultas-procedimientos-pasos` dentro de `PermisosApiFilter`, solo para `retroceso`, y no
     usar `abierta()`, que exige médico, sino una variante para empleados. Es más superficie de
     permisos, y avanzar (`finalizacion`) seguiría siendo solo de DOCTOR.
2. **Pasos ramificados** (un paso con varios anteriores, o un anterior con varios sucesores):
   - **(A, recomendada)** 409 `consulta.retrocesoRamificado`. Reabrir el anterior dejaría un
     hermano PENDIENTE sin su requisito completo, y no hay una regla clara de cuál reabrir.
     Los procedimientos actuales, incluida la demo, son lineales.
   - (B) Borrar también los hermanos vacíos. Es más complejo y el contrato no lo pide.
3. **Respuesta 200:**
   - **(A, recomendada)** El DTO del paso anterior reabierto, como las otras acciones.
   - (B) 204 sin cuerpo.

## Pruebas (TDD: en rojo primero)

`AtencionServiceTest` (con el estilo actual, `EntityManager` simulado):

1. Caso feliz: paso 2 PENDIENTE sin valor ni órdenes y paso 1 COMPLETADO. Se espera:
   - `em.remove(paso2)`;
   - el paso 1 queda PENDIENTE, con `fechaFin` null y **el mismo valor**;
   - se devuelve el paso 1.
2. Primer paso, sin predecesor SIGUIENTE: 409 `consulta.retrocesoPrimerPaso`, sin `remove`.
3. El paso actual tiene valor: 409 `consulta.retrocesoConRegistros`. Un valor en blanco cuenta
   como vacío.
4. El paso actual tiene una orden de examen: 409 `consulta.retrocesoConRegistros`.
5. El paso actual está COMPLETADO: 409 `consulta.pasoYaCompletado`.
6. El anterior no está COMPLETADO: 409 `consulta.retrocesoAnteriorPendiente`.
7. Dos predecesores, o un hermano ya creado: 409 `consulta.retrocesoRamificado`.
8. Una secuencia que no es SIGUIENTE no cuenta como anterior. Se verifica el
   `tipoSecuencia='SIGUIENTE'` del JPQL y su parámetro.
9. Consulta cerrada: 409 `consulta.yaCerrada`. Otra clínica: `consulta.otraClinica`. Sin rol
   médico: `consulta.soloMedico`. En los tres casos no hay `remove` ni cambios.
10. Paso de otra consulta: `consulta.pasoPropio`.
11. Paso sin definición: `consulta.pasoSinDefinicionHistorica`.
12. Se pide el bloqueo `PESSIMISTIC_WRITE` de la consulta, igual que en `completar`.

`RestApiTest`:

13. `PUT consultas-procedimientos-pasos/{id}/retroceso` llama a `atencion.retroceder` con la
    consulta del paso, y responde 200 con el DTO devuelto.
14. La ruta anidada `consultas-procedimientos/{cp}/pasos/{id}/retroceso` hace lo mismo. Si el
    paso no es de ese procedimiento, da 404.
15. Cada código nuevo llega como 409 con su `code`: un caso por código en el parametrizado
    `erroresDeServicios`.

`PermisosApiHttpTest` (filtro con el arnés existente):

16. DOCTOR de la clínica del paso: pasa como acción.
17. ADMINISTRACION: 403 `auth.permiso` (decisión 1A).
18. Paso de otra clínica: 403 sin llegar al recurso.
19. `PUT /consultas-procedimientos-pasos/{id}/otra-cosa` sigue denegado: solo se agregó
    `retroceso`.

`AutenticacionFilterTest`: el PUT a `…/retroceso` sin token CSRF da 403, en el
`@ValueSource` existente.

Prueba contra Liberty al final, solo si lo autorizas, porque **escribe datos clínicos de la
demo**. Sobre la consulta de hoy de la clínica 1 (`…-2`: paso 1 COMPLETADO con valor y paso 2
PENDIENTE vacío):
- retroceso del paso 2: 200; el paso 1 queda PENDIENTE con su valor y el paso 2 ya no existe;
- retroceso del paso 1: 409 `consulta.retrocesoPrimerPaso`;
- volver a completar el paso 1 con su valor, y comprobar que el paso 2 se recrea.

Al terminar, `scripts/reiniciar-demo.sh` deja la demo como estaba. Usa docker, así que lo
ejecutarías tú.

## Archivos (8, por eso el plan)

| Archivo | Cambio |
| --- | --- |
| `control/service/AtencionService.java` | `retroceder(UUID consulta, UUID paso)`. |
| `boundary/resources/ConsultaProcedimientoPasoResource.java` | `PUT {id}/retroceso`. |
| `boundary/resources/provider/PermisosApiFilter.java` | `"retroceso"` en las acciones. |
| `boundary/resources/provider/ApiExceptionMapper.java` | 4 claves nuevas en `CONFLICT` (`retrocesoPrimerPaso`, `retrocesoConRegistros`, `retrocesoAnteriorPendiente`, `retrocesoRamificado`). |
| `test/.../AtencionServiceTest.java`, `RestApiTest.java`, `PermisosApiHttpTest.java`, `AutenticacionFilterTest.java` | Pruebas de arriba. |

Más `docs/api-rest.md` (fila en la tabla de acciones, los 409 y el efecto sobre el valor) y
`openapi.json`. Sin cambio de esquema. Commits: (1) servicio y mapper con sus pruebas; (2) ruta
y filtro con sus pruebas; (3) documentación. `mvn test` antes de cada uno. Sin push.

## HU4 en REST: qué existe y qué falta

HU4: "registrando la atención brindada, exámenes administrados de la lista de los que pueden
asignarse, y una vez completado, avanzar al siguiente paso si no fuera el último o retroceder
al anterior si no fuera el primer paso."

| Necesidad | Estado | Ruta o lo que falta |
| --- | --- | --- |
| Ver la consulta y sus pasos | **Existe** | `GET /api/consultas/{id}`, `GET /api/consultas/{id}/procedimientos`, `GET /api/consultas-procedimientos/{id}/pasos` (traen `valor`) y el dashboard de B3. Para retomarla: `PUT /api/consultas/{id}/sesion`. |
| Registrar la atención de un paso | **Existe, pero junta registrar y avanzar** | `PUT /api/consultas-procedimientos-pasos/{id}/finalizacion` con `{valor}` guarda el valor, completa el paso y crea los siguientes en una sola acción. Las notas de la consulta van por `PATCH /api/consultas/{id}`. **Falta** guardar el valor sin completar el paso (un borrador): el PUT genérico del paso lo bloquea `PermisosApiFilter`. Propuesta: `PUT …/{id}/atencion` con `{valor}`, solo para un paso PENDIENTE, en un B5. Hoy, un paso PENDIENTE solo puede tener valor si se reabrió con retroceso. |
| Avanzar al siguiente paso | **Existe** | El mismo `PUT …/{id}/finalizacion`. En el último paso (`indicaFin`) no crea sucesores. Después se cierra con `PUT /api/consultas/{id}/cierre`. |
| Retroceder al anterior | **Falta** | Esta tarea. |
| Listar los exámenes asignables a un paso | **Existe solo en parte** | `GET /api/procedimientos-pasos/{idProcedimientoPaso}/examenes-configurados` (catálogo; el `idProcedimientoPaso` viene en el DTO del paso). Devuelve solo `idExamen`, sin el nombre, e incluye asociaciones inactivas: la SPA tendría que pedir `GET /api/examenes/{id}` por cada uno (N+1) y filtrar. `AtencionService.examenes(consulta)` ya hace la consulta con `JOIN FETCH`, pero no está en REST. **Falta**: `GET /api/consultas-procedimientos-pasos/{id}/examenes-asignables` → `[{idExamen, nombre, observaciones}]` con asociación y examen activos, en una sola consulta. |
| Registrar un examen nuevo | Existe, con una salvedad | `POST …/{id}/examenes` crea un examen nuevo **y lo agrega al catálogo del procedimiento** (`ProcedimientoPasoExamen`). Eso afecta a todas las consultas futuras. No sirve para "de la lista de los que pueden asignarse". |
| Emitir órdenes de examen | **Existe, pero sin examen** | `POST /api/consultas-procedimientos-pasos/{id}/ordenes-examen` con `{indicaciones}` (solo en un paso PENDIENTE); listar: `GET /api/ordenes-examen?idConsultaProcedimientoPaso=`; resultados: `POST /api/ordenes-examen/{id}/resultados`. **Falta** que la orden diga **qué examen** se ordenó: `orden_examen` no tiene `id_examen` (solo `indicaciones` de texto), ni en el esquema ni en la entidad. Hacerlo requiere **una migración nueva** (columna `id_examen` opcional con FK a `examen`), la entidad, el DTO, y validar que el examen sea asignable al paso. Mientras tanto, la SPA solo puede poner el nombre del examen dentro de `indicaciones`. |

Propongo dejar esas tres brechas para otra tarea (B5), porque una necesita migración:

1. `PUT …/{id}/atencion` (borrador del valor);
2. `GET …/{id}/examenes-asignables`;
3. la orden con `idExamen`, que es la de la migración.

Las decides tú; no entran en B4 salvo que lo pidas.
