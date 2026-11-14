# Plan B5: exámenes asignables a un paso y órdenes con examen (HU4)

Fecha: 2026-11-13. Rama: `backend-rubrica`. Estado: **esperando aprobación**. No se ha
cambiado código. Fuera de alcance, como pediste: guardar el valor de un paso sin completarlo.

## Lo que hay hoy

- **Catálogo:** `procedimiento_paso_examen` asocia exámenes con la **definición** del paso
  (`id_procedimiento_paso`), con `activo`. `examen` también tiene `activo`.
  `AtencionService.examenes(consulta)` trae las asociaciones de todos los pasos de una consulta,
  pero no está en REST ni filtra por `activo`.
- **REST hoy:** `GET /api/procedimientos-pasos/{idProcedimientoPaso}/examenes-configurados`
  devuelve solo `idExamen`, sin el nombre, e incluye asociaciones inactivas.
- **Órdenes:** `orden_examen` tiene solo `id_orden_examen`, `id_consulta_procedimiento_paso`,
  `fecha_creacion` e `indicaciones`. No hay `id_examen` ni en el esquema ni en la entidad.
- **Cómo entra el POST de una orden hoy:** `POST /api/consultas-procedimientos-pasos/{id}/ordenes-examen`
  → `OrdenExamenResource.crearEntidad` → `AtencionService.ordenar(consulta, paso, indicaciones)`.
  `ordenar` exige consulta abierta, paso propio y PENDIENTE, e indicaciones de 1 a 255 caracteres.
- **Hallazgo de B4, contra Liberty:** ese POST anidado **exige `idConsultaProcedimientoPaso`
  también en el cuerpo** (`@NotNull` en `OrdenExamenDto`). Sin él da
  `400 validacion.invalida`, aunque el id ya va en la ruta. El ejemplo de `api-rest.md`
  (`{"indicaciones":"Hemograma"}`) no funciona tal cual.
- **Vista JSF:** `AtencionModel.ordenar()` crea órdenes solo con indicaciones. `consulta.xhtml`
  no tiene selector de examen.
- **Datos demo:** no hay exámenes ni asociaciones `procedimiento_paso_examen`. Con lo que hay,
  la lista de asignables de la demo saldría vacía.
- **`PermisosApiFilter`:** un tercer segmento que no sea una acción conocida en
  `consultas-procedimientos-pasos/{id}/…` se deniega (403).

## (a) Exámenes asignables a un paso

```
GET /api/consultas-procedimientos-pasos/{id}/examenes-asignables      (cookie; también anidada)
200 [{"idExamen":"…","nombre":"Hemograma"}, …]     orden por nombre y luego idExamen
```

- **Asignable** significa que la asociación `procedimiento_paso_examen` es de la definición de
  este paso y está activa, y que el examen está activo.
- **Una sola consulta de datos**, una proyección:
  ```
  SELECT DISTINCT e.idExamen, e.nombre
  FROM ConsultaProcedimientoPaso p JOIN p.idProcedimientoPaso def,
       ProcedimientoPasoExamen pe JOIN pe.idExamen e
  WHERE p.idConsultaProcedimientoPaso = :paso AND pe.idProcedimientoPaso = def
    AND pe.activo = true AND e.activo = true
  ORDER BY e.nombre, e.idExamen
  ```
  Las comprobaciones de permiso no cuentan como consultas de datos:
  - el filtro: sesión, asignación y clínica del paso;
  - `cargar(consulta)`: clínica del rol activo y rol médico.
- Un paso sin definición, o una definición sin asociaciones activas, da `200 []`.
- Se lista en cualquier estado del paso y de la consulta, porque solo lee. Ordenar sí exige
  paso PENDIENTE.
- **Permisos:** solo DOCTOR, como el resto de los recursos clínicos. `"examenes-asignables"`
  se agrega a las acciones de paso de `PermisosApiFilter`; un GET entra como lectura del recurso
  `consultas-procedimientos-pasos`.
- **Implementación:**
  - `AtencionService.examenesAsignables(consulta, paso)`: `cargar`, `pasoPropio` y la
    proyección, que devuelve `record ExamenAsignable(UUID idExamen, String nombre)`;
  - `ConsultaProcedimientoPasoResource`: `GET {id}/examenes-asignables`.

## (b) Migración y órdenes con examen

### Migración `docs/migrations/20261113_orden_examen_examen.sql`

Con el estilo de `20261112_documento_tipo_valor_unico.sql`: transacción, `lock_timeout`, y se
puede ejecutar varias veces.

```sql
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.orden_examen ADD COLUMN IF NOT EXISTS id_examen uuid;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='public.orden_examen'::regclass
                 AND conname='fk_orden_examen_examen') THEN
    ALTER TABLE public.orden_examen ADD CONSTRAINT fk_orden_examen_examen
      FOREIGN KEY (id_examen) REFERENCES public.examen(id_examen) ON UPDATE CASCADE ON DELETE RESTRICT;
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS ix_orden_examen_examen ON public.orden_examen (id_examen);
COMMENT ON COLUMN public.orden_examen.id_examen IS 'Examen ordenado (HU4). Null en órdenes anteriores a esta migración.';
COMMIT;
```

- **Columna nullable:** las órdenes existentes no tienen examen. La obligatoriedad la impone
  el POST, no la base.
- **`ON DELETE RESTRICT`,** igual que las demás FK del esquema: no se puede borrar un examen
  que tenga órdenes.
- **Prueba SQL** `docs/migrations/tests/orden_examen_examen_test.sql`:
  - la columna existe, es `uuid` y nullable;
  - la FK y el índice existen;
  - un `id_examen` inexistente se rechaza;
  - una orden sin examen se acepta;
  - ejecutar la migración dos veces no falla.
- `scripts/preparar-bd-desarrollo.sh`: aplicar la migración después de
  `20261112_documento_tipo_valor_unico.sql`.

### Datos demo de exámenes

En un archivo nuevo, `docs/migrations/20261113_datos_desarrollo_examenes.sql`, con UUID fijos y
`ON CONFLICT DO NOTHING`, aplicado por `preparar-bd-desarrollo.sh` después de los datos clínicos:

- 3 exámenes: "Hemograma completo", "Glucosa en ayunas" y "Examen general de orina", con su
  clasificación en un tipo de examen existente o en uno demo;
- asociaciones activas a "Evaluación médica" (paso 2);
- 1 asociación **inactiva** a "Evaluación médica" y 1 examen **inactivo** asociado, para que la
  prueba demuestre el filtro.

Sin esto no se puede probar (a) contra Liberty. Es solo catálogo: no toca consultas ni pasos,
y `reiniciar-demo.sh` no necesita cambios, porque ya borra las órdenes de las consultas demo.

### Entidad, DTO y POST

- `OrdenExamen`: `@ManyToOne @JoinColumn(name="id_examen") Examen idExamen`.
- `OrdenExamenDto`: `idExamen` (UUID). `toDto` y `aplicar` lo mapean.
- `AtencionService.ordenar(consulta, paso, idExamen, indicaciones)`:
  1. `abierta`, `pasoPropio` y paso PENDIENTE, como hoy;
  2. `idExamen` null: 400 `consulta.examenObligatorio`;
  3. el examen no es asignable a la definición de ese paso: 400 `consulta.examenNoAsignable`. Se
     comprueba con un `COUNT` de la misma condición que (a), con el par paso/examen como
     parámetros. Inexistente, inactivo, asociación inactiva o examen de otro paso dan el mismo
     error;
  4. `indicaciones` según la decisión 2.
- `OrdenExamenResource.crearEntidad` (POST anidado) pasa `datos.getIdExamen()`.
- **Vista JSF:** según la decisión 1. Las claves `consulta.examen*` son solo de REST, como
  `citas.rango`, así que no van al bundle i18n.

### Lo que la migración implica para Liberty

En cuanto la entidad mapee `id_examen`, **toda** consulta JPA sobre `OrdenExamen` incluye esa
columna. Liberty, con el código nuevo, falla al leer o crear órdenes en una base sin la
migración. Orden de despliegue:

1. corres la migración (docker, la ejecutas tú);
2. después se despliega el código.

Para la prueba con curl te pediré que apliques las dos migraciones antes de que Liberty
recargue el commit de la entidad. Liberty recarga solo al compilar, así que lo coordinamos.

## Decisiones que necesito de ti

1. **Vista JSF de órdenes:**
   - **(A, recomendada)** Sin cambios. JSF sigue creando órdenes sin examen (la columna es
     nullable) mediante un `ordenar(consulta, paso, indicaciones)` que se conserva para JSF.
     Solo REST exige `idExamen`.
   - (B) Agregar a `consulta.xhtml` un selector de examen asignable y exigirlo también en JSF.
     Son 2 archivos más y claves en los 6 idiomas.
2. **`indicaciones` cuando hay examen:**
   - **(A, recomendada)** Opcional, de hasta 255 caracteres, porque el examen ya identifica la
     orden.
   - (B) Sigue siendo obligatorio.
3. **`idConsultaProcedimientoPaso` en el cuerpo del POST anidado** (hallazgo de B4):
   - **(A, recomendada)** En la ruta anidada se toma de la URL. Si el cuerpo lo trae y es
     distinto, da 400 `id.inconsistente`, el mismo código que usa el PUT. Así el ejemplo
     documentado funciona.
   - (B) Dejarlo como está y corregir solo el ejemplo de `api-rest.md`.
4. **Datos demo de exámenes:** archivo nuevo, como arriba (recomendado), o ninguno, y probar
   (a) solo con JUnit.

## Pruebas (TDD: en rojo primero)

`AtencionServiceTest`:

1. Asignables: la proyección filtra por paso, definición, `pe.activo` y `e.activo`, con
   parámetros. Se verifica el JPQL y el `setParameter("paso", …)`.
2. Asignables: devuelve los `record` en el orden de la consulta. Un paso sin definición da `[]`
   sin consultar.
3. Asignables: un paso de otra consulta da `consulta.pasoPropio`; otra clínica,
   `consulta.otraClinica`; rol no médico, `consulta.soloMedico`.
4. Ordenar con un examen asignable: persiste la orden con `idExamen`, indicaciones recortadas y
   fecha.
5. Ordenar sin `idExamen`: 400 `consulta.examenObligatorio`, sin `persist`.
6. Ordenar con un examen no asignable: 400 `consulta.examenNoAsignable`, sin `persist`. El
   `COUNT` usa las mismas condiciones de actividad.
7. Ordenar en un paso completado: sigue dando 409 `consulta.ordenPasoCompletado`.
8. Ordenar sin indicaciones y con examen (decisión 2A): se acepta. Indicaciones de 256
   caracteres: 400.
9. La sobrecarga para JSF (decisión 1A) sigue funcionando sin examen.

`RestApiTest`:

10. `GET …/{id}/examenes-asignables`: 200, llaves exactas `idExamen` y `nombre`; ruta anidada.
11. `POST …/{id}/ordenes-examen` con `{"idExamen":…}` sin `idConsultaProcedimientoPaso`
    (decisión 3A): 201, `idExamen` en la respuesta, y el servicio se llama con el paso de la URL.
12. Mismo POST con un `idConsultaProcedimientoPaso` distinto en el cuerpo: 400
    `id.inconsistente`, sin llamar al servicio.
13. Los 400 del servicio llegan con su código.
14. `GET /api/ordenes-examen/{id}` incluye `idExamen`, que puede ser null en órdenes antiguas.

`PermisosApiHttpTest`:

15. DOCTOR lee `…/examenes-asignables` de un paso de su clínica: 200. ADMINISTRACION: 403. Un
    paso de otra clínica: 403.

La prueba SQL de la migración (`orden_examen_examen_test.sql`, descrita arriba) se ejecuta en un
PostgreSQL local temporal sin docker, si está disponible en la máquina, como en la tarea 2. Si no
lo está, solo se revisa la sintaxis y lo indico.

Contra Liberty, cuando apliques las migraciones, con `doctor.demo` en la consulta `…-2` de la
clínica 1:
- asignables del paso 2: los 3 activos, sin el inactivo ni el de la asociación inactiva;
- orden con un examen asignable: 201;
- orden con el examen inactivo: 400 `consulta.examenNoAsignable`;
- orden sin `idExamen`: 400 `consulta.examenObligatorio`;
- el retroceso de ese paso da 409 `consulta.retrocesoConRegistros` por la orden.

## Archivos (unos 16, por eso el plan)

| Archivo | Cambio |
| --- | --- |
| `docs/migrations/20261113_orden_examen_examen.sql` y `tests/orden_examen_examen_test.sql` | Migración y su prueba. |
| `docs/migrations/20261113_datos_desarrollo_examenes.sql` | Catálogo demo (decisión 4). |
| `scripts/preparar-bd-desarrollo.sh` | Aplicar los dos archivos nuevos. |
| `entity/OrdenExamen.java` | `idExamen`. |
| `boundary/resources/dto/OrdenExamenDto.java` | `idExamen`. |
| `boundary/resources/OrdenExamenResource.java` | `toDto`/`aplicar`; POST anidado con `idExamen` y el padre tomado de la URL (decisión 3A). |
| `control/service/AtencionService.java` | `examenesAsignables` y `ordenar` con examen; conserva la sobrecarga para JSF. |
| `boundary/resources/ConsultaProcedimientoPasoResource.java` | `GET {id}/examenes-asignables`. |
| `boundary/resources/provider/PermisosApiFilter.java` | `"examenes-asignables"` en las acciones. |
| Pruebas: `AtencionServiceTest`, `RestApiTest`, `PermisosApiHttpTest` | Las de arriba. |
| `docs/api-rest.md`, `openapi.json` | Rutas, `idExamen`, errores, ejemplo de la orden corregido y el orden de despliegue. |

## Commits

1. Migración, su prueba SQL, datos demo y script, sin código Java (Liberty sigue funcionando
   sin aplicarla).
2. `GET …/examenes-asignables`, que no depende de la columna nueva, con sus pruebas.
3. `idExamen` en la entidad, el DTO y el POST de órdenes, con sus pruebas. **Desde aquí
   Liberty necesita la migración.**
4. Documentación.

`mvn test` antes de cada commit. Sin push.
