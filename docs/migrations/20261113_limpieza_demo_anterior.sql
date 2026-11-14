\set ON_ERROR_STOP on
-- Borra los datos demo anteriores (Clínica de demostración, admin.demo, doctor.demo,
-- paciente.demo, su procedimiento, sus 7 consultas y su catálogo de exámenes: UUID de110000…
-- a de11000f). Solo para bases de desarrollo que los cargaron con 20261111_datos_desarrollo.sql,
-- 20261112_datos_desarrollo_clinicos.sql y 20261113_datos_desarrollo_examenes.sql.
-- scripts/preparar-bd-desarrollo.sh NO lo ejecuta: haga antes un respaldo (docs/LEVANTAR.md).
-- psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261113_limpieza_demo_anterior.sql
--
-- También borra lo que cuelga de esas filas y se creó desde la app: consultas de las personas demo
-- con sus procedimientos, pasos, órdenes y resultados; documentos, contactos, cuentas y
-- asignaciones de las personas demo; pasos, secuencias y exámenes asignables del procedimiento demo.
-- Si alguna fila de otra persona depende de la demo (una asignación en la clínica demo, un paso de
-- otra consulta hecho por un empleado demo o del procedimiento demo, una orden de un examen demo…)
-- se detiene sin borrar nada y dice cuántas hay. El tipo de documento DUI (de110004) se conserva:
-- es catálogo y los datos nuevos lo reutilizan. Se puede ejecutar varias veces.
BEGIN;
SET LOCAL lock_timeout = '5s';

CREATE TEMP TABLE lim_persona ON COMMIT DROP AS
SELECT id_persona AS id FROM public.persona WHERE id_persona::text LIKE 'de110001-%';
CREATE TEMP TABLE lim_clinica ON COMMIT DROP AS
SELECT id_clinica AS id FROM public.clinica WHERE id_clinica::text LIKE 'de110000-%';
CREATE TEMP TABLE lim_pr ON COMMIT DROP AS
SELECT id_persona_rol AS id FROM public.persona_rol
WHERE id_persona_rol::text LIKE 'de110002-%' OR id_persona IN (SELECT id FROM lim_persona);
CREATE TEMP TABLE lim_consulta ON COMMIT DROP AS
SELECT id_consulta AS id FROM public.consulta
WHERE id_consulta::text LIKE 'de110009-%' OR id_persona_rol IN (SELECT id FROM lim_pr);
CREATE TEMP TABLE lim_cp ON COMMIT DROP AS
SELECT id_consulta_procedimiento AS id FROM public.consulta_procedimiento
WHERE id_consulta_procedimiento::text LIKE 'de11000a-%' OR id_consulta IN (SELECT id FROM lim_consulta);
CREATE TEMP TABLE lim_paso ON COMMIT DROP AS
SELECT id_consulta_procedimiento_paso AS id FROM public.consulta_procedimiento_paso
WHERE id_consulta_procedimiento_paso::text LIKE 'de11000b-%' OR id_consulta_procedimiento IN (SELECT id FROM lim_cp);
CREATE TEMP TABLE lim_orden ON COMMIT DROP AS
SELECT id_orden_examen AS id FROM public.orden_examen WHERE id_consulta_procedimiento_paso IN (SELECT id FROM lim_paso);
CREATE TEMP TABLE lim_proc ON COMMIT DROP AS
SELECT id_procedimiento AS id FROM public.procedimiento WHERE id_procedimiento::text LIKE 'de110006-%';
CREATE TEMP TABLE lim_pdef ON COMMIT DROP AS
SELECT id_procedimiento_paso AS id FROM public.procedimiento_paso
WHERE id_procedimiento_paso::text LIKE 'de110007-%' OR id_procedimiento IN (SELECT id FROM lim_proc);
CREATE TEMP TABLE lim_examen ON COMMIT DROP AS
SELECT id_examen AS id FROM public.examen WHERE id_examen::text LIKE 'de11000c-%';
CREATE TEMP TABLE lim_tipo ON COMMIT DROP AS
SELECT id_tipo_examen AS id FROM public.tipo_examen WHERE id_tipo_examen::text LIKE 'de11000d-%';

-- Filas ajenas que dependen de la demo: si hay alguna, no se borra nada.
DO $$
DECLARE
    ajenas text := concat_ws('; ',
        (SELECT nullif(count(*), 0) || ' asignación(es) de otras personas en la clínica demo'
         FROM public.persona_rol WHERE id_clinica IN (SELECT id FROM lim_clinica) AND id_persona_rol NOT IN (SELECT id FROM lim_pr)),
        (SELECT nullif(count(*), 0) || ' procedimiento(s) de otras consultas con el procedimiento demo'
         FROM public.consulta_procedimiento WHERE id_procedimiento IN (SELECT id FROM lim_proc)
           AND id_consulta_procedimiento NOT IN (SELECT id FROM lim_cp)),
        (SELECT nullif(count(*), 0) || ' paso(s) de otras consultas hechos por un empleado demo o del procedimiento demo'
         FROM public.consulta_procedimiento_paso WHERE id_consulta_procedimiento_paso NOT IN (SELECT id FROM lim_paso)
           AND (id_persona_rol IN (SELECT id FROM lim_pr) OR id_procedimiento_paso IN (SELECT id FROM lim_pdef))),
        (SELECT nullif(count(*), 0) || ' orden(es) de otras consultas con un examen demo'
         FROM public.orden_examen WHERE id_examen IN (SELECT id FROM lim_examen) AND id_orden_examen NOT IN (SELECT id FROM lim_orden)),
        (SELECT nullif(count(*), 0) || ' examen(es) asignable(s) de otros pasos con un examen demo'
         FROM public.procedimiento_paso_examen WHERE id_examen IN (SELECT id FROM lim_examen)
           AND id_procedimiento_paso NOT IN (SELECT id FROM lim_pdef)),
        (SELECT nullif(count(*), 0) || ' examen(es) de otros catálogos clasificados en el tipo demo'
         FROM public.examen_tipo_examen WHERE id_tipo_examen IN (SELECT id FROM lim_tipo) AND id_examen NOT IN (SELECT id FROM lim_examen)),
        (SELECT nullif(count(*), 0) || ' secuencia(s) que unen un paso demo con otro paso'
         FROM public.procedimiento_paso_secuencia
         WHERE (id_procedimiento_paso IN (SELECT id FROM lim_pdef)) <> (id_procedimiento_paso_referencia IN (SELECT id FROM lim_pdef))));
BEGIN
    IF ajenas <> '' THEN
        RAISE EXCEPTION 'No se borró nada: hay datos de otras personas que dependen de la demo anterior (%). Revíselos o restaure el respaldo.', ajenas;
    END IF;
END $$;

DELETE FROM public.examen_resultado WHERE id_orden_examen IN (SELECT id FROM lim_orden);
DELETE FROM public.orden_examen WHERE id_orden_examen IN (SELECT id FROM lim_orden);
DELETE FROM public.consulta_procedimiento_paso WHERE id_consulta_procedimiento_paso IN (SELECT id FROM lim_paso);
DELETE FROM public.consulta_procedimiento WHERE id_consulta_procedimiento IN (SELECT id FROM lim_cp);
DELETE FROM public.consulta WHERE id_consulta IN (SELECT id FROM lim_consulta);
DELETE FROM public.procedimiento_paso_examen
WHERE id_procedimiento_paso IN (SELECT id FROM lim_pdef) OR id_examen IN (SELECT id FROM lim_examen);
DELETE FROM public.procedimiento_paso_secuencia
WHERE id_procedimiento_paso IN (SELECT id FROM lim_pdef) OR id_procedimiento_paso_referencia IN (SELECT id FROM lim_pdef);
DELETE FROM public.procedimiento_paso WHERE id_procedimiento_paso IN (SELECT id FROM lim_pdef);
DELETE FROM public.procedimiento WHERE id_procedimiento IN (SELECT id FROM lim_proc);
DELETE FROM public.examen_tipo_examen
WHERE id_examen IN (SELECT id FROM lim_examen) OR id_tipo_examen IN (SELECT id FROM lim_tipo);
DELETE FROM public.examen WHERE id_examen IN (SELECT id FROM lim_examen);
DELETE FROM public.tipo_examen WHERE id_tipo_examen IN (SELECT id FROM lim_tipo);
DELETE FROM public.usuario WHERE id_persona IN (SELECT id FROM lim_persona);
DELETE FROM public.documento WHERE id_persona IN (SELECT id FROM lim_persona);
DELETE FROM public.medio_contacto WHERE id_persona IN (SELECT id FROM lim_persona);
DELETE FROM public.persona_rol WHERE id_persona_rol IN (SELECT id FROM lim_pr);
DELETE FROM public.persona WHERE id_persona IN (SELECT id FROM lim_persona);
DELETE FROM public.clinica WHERE id_clinica IN (SELECT id FROM lim_clinica);

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM public.clinica WHERE id_clinica::text LIKE 'de110000-%')
       OR EXISTS (SELECT 1 FROM public.persona WHERE id_persona::text LIKE 'de110001-%')
       OR EXISTS (SELECT 1 FROM public.persona_rol WHERE id_persona_rol::text LIKE 'de110002-%')
       OR EXISTS (SELECT 1 FROM public.usuario WHERE id_usuario::text LIKE 'de110003-%')
       OR EXISTS (SELECT 1 FROM public.documento WHERE id_documento::text LIKE 'de110005-%')
       OR EXISTS (SELECT 1 FROM public.consulta WHERE id_consulta::text LIKE 'de110009-%')
       OR EXISTS (SELECT 1 FROM public.procedimiento WHERE id_procedimiento::text LIKE 'de110006-%')
       OR EXISTS (SELECT 1 FROM public.examen WHERE id_examen::text LIKE 'de11000c-%') THEN
        RAISE EXCEPTION 'Quedaron filas de la demo anterior; no se guardó ningún cambio.';
    END IF;
END $$;
COMMIT;
\echo 'Demo anterior eliminada (o ya no estaba). Siga con scripts/preparar-bd-desarrollo.sh.'
