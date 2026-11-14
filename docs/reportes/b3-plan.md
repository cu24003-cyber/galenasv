# Plan B3: dashboard de la HU3

Fecha: 2026-11-13. Rama: `backend-rubrica`. Estado: **esperando aprobación**. No se ha
cambiado código.

## Lo que pide la HU3 y lo que hay hoy

HU3 (`../ChivoMed/docs/rubrica.md`): el empleado elige rol y clínica (B2 ya da
`GET /api/atencion/asignaciones` y `PUT /api/atencion/rol-activo`). Después ve las consultas de
hoy, con filtro por fechas. Para cada consulta se muestran las generales del paciente, la fecha
de inicio y los procedimientos, cada uno con sus pasos en jerarquía: quién lo realizó, cuándo,
y una opción para ver lo que se registró en el paso. Desde ahí se elige la consulta a atender.

`AtencionService.historial(desde, hasta)`:

- Exige `medicoActivo()`, que relee de la base el `PersonaRol` de `AtencionSesion.getRolActivo()`.
  Solo acepta DOCTOR; si no, lanza `consulta.soloMedico` (400). Sin rol activo lanza
  `consulta.medicoClinica` (400).
- Filtra por la clínica de ese rol, convierte los días de El Salvador a UTC con `hasta`
  incluido, y lanza `consulta.rangoInvalido` (400) si el rango está invertido.
- Trae `Consulta` con `JOIN FETCH` de paciente, persona y clínica, ordenadas por
  `fechaInicio DESC`.
- Con `desde`/`hasta` nulos **no limita el rango** (devuelve todo el historial).
- **No revisa `AccesoSesion`.** No comprueba que haya sesión vigente ni que el rol activo
  siga perteneciendo a la cuenta.

`PermisosApiFilter` deja pasar `/atencion` sin revisar el rol. `AutenticacionFilter` exige la
cookie, pero, igual que en B2, el servicio debe validar por sí mismo.

## Rutas

```
GET /api/atencion/dashboard?desde=yyyy-MM-dd&hasta=yyyy-MM-dd
GET /api/atencion/dashboard/pasos/{idConsultaProcedimientoPaso}/valor
```

- Son GET porque solo leen y las fechas no son datos sensibles. Requieren cookie de sesión y no
  CSRF, como todo GET.
- Fechas por defecto, en días de El Salvador con `hasta` incluido:
  - sin `desde` ni `hasta`: hoy;
  - con solo uno de los dos: ese mismo día;
  - fecha mal escrita: 400 `dashboard.fecha`;
  - rango invertido: 400 `consulta.rangoInvalido` (lo lanza `historial`);
  - rango mayor de 31 días: 400 `dashboard.rango` (ver decisión 3).
- La ruta del valor es la única que devuelve `valor`. El dashboard solo informa `tieneValor`.

## DTO (`boundary/resources/dto/DashboardDto.java`, records)

```json
[
  {
    "idConsulta": "…",
    "fechaInicio": "2026-11-13T08:00:00-06:00",
    "fechaFin": null,
    "paciente": {
      "nombres": "…", "apellidos": "…",
      "fechaNacimiento": "1990-05-02", "edad": 36,
      "documentos": [{"tipoDocumento": "DUI", "numero": "00000000-3"}]
    },
    "procedimientos": [
      {
        "idConsultaProcedimiento": "…",
        "nombre": "Consulta general de demostración",
        "fechaInicio": "…", "fechaFin": null,
        "pasos": [
          {
            "idConsultaProcedimientoPaso": "…",
            "nombre": "Toma de signos vitales",
            "orden": 1, "nivel": 0,
            "estado": "COMPLETADO",
            "responsable": {"nombre": "…", "rol": "Doctor"},
            "fechaInicio": "…", "fechaFin": "…",
            "tieneValor": true
          }
        ]
      }
    ]
  }
]
```

Valor de un paso:

```json
{"idConsultaProcedimientoPaso": "…", "nombre": "Toma de signos vitales", "estado": "COMPLETADO",
 "fechaFin": "…", "valor": "120/80"}
```

Detalles:

- Fechas con offset `-06:00`, como en HU1. `fechaNacimiento` es `yyyy-MM-dd`. `edad` se calcula
  en años cumplidos a hoy en El Salvador y es `null` si no hay fecha de nacimiento.
- `documentos` lleva **todos** los documentos del paciente, ordenados por nombre del tipo, para
  no elegir uno arbitrario. `numero` es el `valor` del documento; no incluye `rutaFisica`.
- `fechaFin` de la consulta **no estaba en tu lista**. Lo agrego porque `fechaFin == null`
  indica que la consulta está abierta, que es lo que la pantalla necesita para "seleccionar una
  consulta para atenderla" (luego `PUT /api/consultas/{id}/sesion`). Lo quito si prefieres.
- `responsable` es el `PersonaRol` asignado al paso: nombre completo de la persona y nombre
  del rol. En un paso COMPLETADO es quien lo realizó y en uno PENDIENTE es quien debe
  realizarlo; `estado` distingue los dos casos. Es `null` si no hay responsable.
- **Orden por secuencia:** por procedimiento, los pasos se ordenan con un orden topológico del
  grafo `ProcedimientoPasoSecuencia` de su catálogo. Los empates se resuelven por `fechaInicio`
  y luego por id. `nivel` es la profundidad del paso en ese grafo y sirve para dibujar la
  jerarquía. Un paso sin definición de catálogo va al final, con `nivel` 0, y `nombre` es `null`. No
  filtro por `tipoSecuencia`, igual que `AtencionService.completar`.
- No se envían `observaciones` ni `referenciaExterna` de la consulta o del procedimiento,
  órdenes de examen ni resultados. Solo lo que pediste.

## Sin N+1: consultas a la base

Una sola respuesta, con un número fijo de consultas sin importar cuántas citas haya:

| # | Consulta | Fuente |
| --- | --- | --- |
| 1 | Consultas del rango con paciente, persona y clínica | `AtencionService.historial(desde, hasta)` sin cambios |
| 2 | Pasos de todas esas consultas: `[idConsulta, idConsultaProcedimiento, nombre procedimiento, fechas del procedimiento, idPaso, idDefinicion, nombre del paso, estado, fechas, nombres y apellidos del responsable, nombre del rol, p.valor IS NOT NULL]` | `DashboardRepository.pasos(List<UUID>)`, proyección `Object[]` con `IN :consultas` y `LEFT JOIN` a definición y responsable |
| 3 | Documentos de los pacientes: `[idPersona, nombre del tipo, valor]` | `DashboardRepository.documentos(List<UUID>)` |
| 4 | Secuencias de los procedimientos que aparecen: `[idPaso origen, idPaso destino]` | `DashboardRepository.secuencias(List<UUID>)` |

- Con `historial` vacío no se ejecutan la 2, la 3 ni la 4.
- Las consultas 2 a 4 son proyecciones, nunca entidades, así que `valor` no se carga. El
  indicador sale de `CASE WHEN p.valor IS NOT NULL THEN TRUE ELSE FALSE END`. El patrón
  `Object[]` con `IN :lista` ya funcionó con EclipseLink en Liberty en HU1.
- Un procedimiento sin pasos aparece con `pasos: []`. Para eso la consulta 2 parte de
  `ConsultaProcedimiento` con `LEFT JOIN` a sus pasos.
- Se suman las consultas de la validación de sesión y rol (abajo). Son 2 o 3, también fijas.

Valor de un paso: una consulta,
`SELECT p.idConsultaProcedimientoPaso, def.nombre, p.estado, p.fechaFin, p.valor … WHERE p.id = :id AND paciente.idClinica.idClinica = :clinica`.
La clínica va en el mismo `WHERE`, así que un paso de otra clínica y un id inexistente dan el
mismo 404 `consulta.pasoNoExiste` sin una consulta previa.

## Sesión y clínica: dónde se validan

En un `DashboardService` nuevo (`@Stateless`), que no cambia `AtencionService` ni la vista JSF
de Historial. Antes de cualquier consulta de datos, `exigirContexto()`:

1. `!acceso.vigente()` → 401 `auth.sesion`.
2. `atencion.getRolActivo() == null` → 403 `auth.rolActivo` ("Seleccione un rol y una clínica").
3. `autenticacion.contexto(acceso.getIdUsuario(), rolActivo.getIdPersonaRol())`: relee el usuario
   y sus asignaciones de empleado activas con rol y clínica activos. Usuario inactivo da 401
   `auth.sesion`; un rol que ya no es suyo o está inactivo da 403 `auth.asignacion`.
4. Código del rol según la decisión 1.
5. Después: `historial(desde, hasta)`, que filtra por la clínica del rol activo leído de la base,
   o la consulta del valor con `:clinica` = la clínica de la asignación validada en el paso 3.

Excepciones nuevas que solo usa REST, como `citas.rango` en HU1. No van al bundle i18n porque
la vista JSF no las muestra, e `I18nTest` exige que cada clave esté en los 6 idiomas:

- `dashboard.fecha` (400), `dashboard.rango` (400): `ServiceException` sin causa.
- `consulta.pasoNoExiste` (404): se agrega a `NOT_FOUND` de `ApiExceptionMapper`.
- `auth.rolActivo` (403): `AccesoException`.

## Decisiones que necesito de ti

1. **¿ADMINISTRACION ve el dashboard?** `historial` solo acepta DOCTOR.
   - **(A, recomendada)** Solo DOCTOR, reutilizando `historial` sin cambios. ADMINISTRACION
     recibe 403 `auth.permiso` desde `DashboardService`, antes de llegar a `historial` y a su
     400 `consulta.soloMedico`.
   - (B) También ADMINISTRACION, con solo lectura. Esto obliga a cambiar `medicoActivo()` dentro
     de `historial` o a no reutilizarlo, y cambia la vista JSF de Historial.
2. **Orden de las consultas:** `historial` las devuelve por `fechaInicio DESC`.
   - **(A, recomendada)** Reordenar en memoria a `fechaInicio ASC` (luego `idConsulta`): para
     "las consultas que debo atender hoy" lo natural es la primera primero.
   - (B) Dejar el orden de `historial`.
3. **Rango máximo:** HU1 permite 366 días, pero aquí cada consulta trae todos sus pasos.
   - **(A, recomendada)** 31 días; si se pasa, 400 `dashboard.rango`.
   - (B) 366 días, igual que HU1.

Además, `historial` lista **todas** las consultas de la clínica, no solo las que tienen un paso
asignado al usuario. Lo dejo así porque pediste reutilizar `historial`. "Las consultas que debo
atender" podría leerse como filtrar por responsable; si lo quieres, sería un `?mias=true`
aparte.

## Archivos (8, por eso el plan)

| Archivo | Cambio |
| --- | --- |
| `entity/repository/DashboardRepository.java` (nuevo) | `pasos`, `documentos`, `secuencias`, `valorPaso`: proyecciones con parámetros. |
| `control/service/DashboardService.java` (nuevo) | `exigirContexto`, fechas por defecto, `consultas(desde, hasta)` (historial + 3 proyecciones, orden topológico, edad) y `valor(idPaso)`. |
| `boundary/resources/dto/DashboardDto.java` (nuevo) | Records `Consulta`, `Paciente`, `Documento`, `Procedimiento`, `Paso`, `Responsable` y `Valor`. |
| `boundary/resources/AtencionResource.java` | Las 2 rutas GET. Las fechas se reciben como `String` y el servicio las convierte, para responder 400 `dashboard.fecha` y no un error de Jersey. |
| `boundary/resources/provider/ApiExceptionMapper.java` | `consulta.pasoNoExiste` en `NOT_FOUND`. |
| `test/.../entity/repository/DashboardRepositoryTest.java` (nuevo) | Ver pruebas. |
| `test/.../control/service/DashboardServiceTest.java` (nuevo) | Ver pruebas. |
| `test/.../boundary/resources/RestApiTest.java` | Ver pruebas. |

Más la documentación: `docs/api-rest.md`, `docs/autenticacion.md` (tabla de rutas de sesión) y
`openapi.json`. Sin cambio de esquema ni migración. No se toca `PermisosApiFilter` ni
`AtencionService`.

## Pruebas (TDD: en rojo primero)

`DashboardServiceTest` (con mocks de `AccesoSesion`, `AtencionSesion`, `AutenticacionService`,
`AtencionService` y `DashboardRepository`):

1. Sin sesión vigente: 401 `auth.sesion` y ninguna llamada a `historial` ni al repositorio.
2. Sin rol activo: 403 `auth.rolActivo`, sin consultas.
3. `autenticacion.contexto` lanza 403 `auth.asignacion` (rol ajeno o inactivo): se propaga sin
   consultar.
4. Rol ADMINISTRACION: 403 `auth.permiso` (decisión 1A), sin llamar a `historial`.
5. Sin fechas: llama a `historial(hoy, hoy)` con hoy en `America/El_Salvador`. Se prueba con
   un `Clock` fijo inyectable cerca de medianoche UTC, donde el día de El Salvador es otro.
6. Solo `desde` o solo `hasta`: usa ese día en ambos extremos.
7. Fecha inválida: 400 `dashboard.fecha`. Rango de 32 días: 400 `dashboard.rango`. Rango de
   31 días: aceptado. En los tres casos se valida antes de consultar.
8. Sin consultas: `[]` y el repositorio nunca se llama (`verify(repo, never())`).
9. **Sin N+1:** con 3 consultas y 2 procedimientos, `pasos`, `documentos` y `secuencias` se
   llaman **una vez cada uno**, con la lista completa de ids.
10. Armado: cada paso cae en su procedimiento y cada procedimiento en su consulta. Un
    procedimiento sin pasos sale con `[]`. `tieneValor` copia el booleano. El DTO no tiene
    campo `valor` (se revisan los componentes del record).
11. Orden topológico: catálogo A→B, A→C, B→D, C→D con pasos creados en desorden. El resultado
    es A, luego B/C por `fechaInicio`, luego D, con `nivel` 0, 1, 1 y 2. Un paso sin
    definición va al final.
12. Orden de las consultas ASC (decisión 2A), aunque `historial` las entregue DESC.
13. Edad: cumpleaños hoy, mañana, el 29 de febrero, y `null` sin fecha de nacimiento.
14. Valor: devuelve los 5 campos. Si el repositorio devuelve vacío (id inexistente u otra
    clínica): 404 `consulta.pasoNoExiste`. Se verifica que se pasó la clínica de la
    asignación validada, no un dato del cliente.
15. Valor sin sesión o con ADMINISTRACION: 401 o 403, sin consultar.

`DashboardRepositoryTest` (EntityManager simulado, como `CitaPublicaRepositoryTest`):

16. Las 3 proyecciones usan `IN :` con parámetro y nunca concatenan ids. Con una lista vacía no
    consultan.
17. El SELECT de `pasos` no contiene `p.valor` como columna, solo dentro del
    `CASE WHEN … IS NOT NULL`. `documentos` no selecciona `rutaFisica`.
18. `valorPaso` filtra por id **y** por clínica en el mismo `WHERE`.

`RestApiTest`:

19. `GET atencion/dashboard` sin parámetros: llama al servicio con `null, null`. Con parámetros,
    los pasa tal cual.
20. JSON: las llaves exactas de consulta, paciente, documento, procedimiento, paso y
    responsable. Ninguna llave `valor`, `observaciones`, `referenciaExterna` ni `rutaFisica`.
    Las fechas llevan offset `-06:00`.
21. Errores del servicio con su código: 401 `auth.sesion`, 403 `auth.permiso`, 400
    `dashboard.rango` y 404 `consulta.pasoNoExiste` en la ruta del valor.
22. `GET …/pasos/{id}/valor` con un id que no es UUID: 404 de Jersey, sin llamar al servicio.

`AutenticacionFilterTest`: agregar `/api/atencion/dashboard` al caso de GET anónimo, que
debe dar 401.

## Commits

1. `DashboardRepository` con sus pruebas.
2. `DashboardService`, `DashboardDto` y las rutas, con sus pruebas, y el cambio en
   `ApiExceptionMapper`.
3. Documentación (`api-rest.md`, `autenticacion.md`, `openapi.json`).

`mvn test` antes de cada commit, con el resultado real. Al final, si Liberty sigue arriba, un
curl con `doctor.demo` (rol inicial DOCTOR de la clínica 1):
- `GET dashboard` sin fechas: debe dar 1 consulta, la de hoy a las 08:00, con el paso 1
  COMPLETADO y `tieneValor: true`, y el paso 2 PENDIENTE.
- Un rango de ±30 días: debe dar 3 consultas.
- `GET …/valor` de un paso de la clínica 2 (con la clínica 1 activa): debe dar 404.
- Las mismas rutas sin sesión: deben dar 401.

Push solo si lo pides.
