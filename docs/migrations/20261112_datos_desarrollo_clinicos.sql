\set ON_ERROR_STOP on
-- Opcional: datos clínicos de demostración para HU1, HU3 y HU4. Solo para desarrollo.
-- Requiere 20261111_datos_desarrollo.sql (clínica 1, cuentas demo y DUI).
-- psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261112_datos_desarrollo_clinicos.sql
--
-- Solo toca filas con UUID fijos de demostración:
--   de110000-…-2 clínica 2           de110002-…-4/5/6 asignaciones nuevas
--   de110006 procedimiento            de110007 pasos            de110008 secuencias
--   de110009 consultas (1-6)          de11000a consulta_procedimiento
--   de11000b pasos ejecutados (consulta*10 + paso)
-- Al repetirlo inserta lo que falte y vuelve a anclar las fechas al día de hoy, sin cambiar
-- estados. Todos los textos son ficticios.
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$ BEGIN
    IF (SELECT count(*) FROM public.persona_rol WHERE id_persona_rol IN (
            'de110002-0000-4000-8000-000000000002', 'de110002-0000-4000-8000-000000000003')) <> 2 THEN
        RAISE EXCEPTION 'Faltan las cuentas demo. Aplique primero 20261111_datos_desarrollo.sql.';
    END IF;
END $$;

-- Instantes relativos al día local de El Salvador: dia(días desde hoy, hora local).
CREATE TEMP TABLE demo_tiempo (base timestamp) ON COMMIT DROP;
INSERT INTO demo_tiempo VALUES (date_trunc('day', now() AT TIME ZONE 'America/El_Salvador'));
CREATE FUNCTION pg_temp.dia(dias integer, hora text) RETURNS timestamptz LANGUAGE sql STABLE AS
$$ SELECT (base + dias * interval '1 day' + hora::interval) AT TIME ZONE 'America/El_Salvador' FROM demo_tiempo $$;

-- Catálogo y asignaciones
INSERT INTO public.clinica (id_clinica,nombre,activo,tipo,comentarios)
VALUES ('de110000-0000-4000-8000-000000000002','Clínica de demostración Norte',true,'Clínica','Entorno local de desarrollo')
ON CONFLICT (id_clinica) DO NOTHING;

-- Las asignaciones nuevas de doctor.demo se fechan después de su DOCTOR de la clínica 1 para
-- que esa siga siendo su rol inicial de login.
INSERT INTO public.persona_rol (id_persona_rol,id_persona,id_rol,id_clinica,fecha_creacion)
SELECT v.id::uuid, v.persona::uuid, r.id_rol, v.clinica::uuid, d.fecha_creacion + v.despues
FROM (VALUES
    ('de110002-0000-4000-8000-000000000004','de110001-0000-4000-8000-000000000003','PACIENTE',
     'de110000-0000-4000-8000-000000000002', interval '0'),
    ('de110002-0000-4000-8000-000000000005','de110001-0000-4000-8000-000000000002','DOCTOR',
     'de110000-0000-4000-8000-000000000002', interval '1 minute'),
    ('de110002-0000-4000-8000-000000000006','de110001-0000-4000-8000-000000000002','ADMINISTRACION',
     'de110000-0000-4000-8000-000000000001', interval '2 minutes')
) AS v(id,persona,codigo,clinica,despues)
JOIN public.rol r ON r.codigo = v.codigo
CROSS JOIN (SELECT fecha_creacion FROM public.persona_rol WHERE id_persona_rol='de110002-0000-4000-8000-000000000002') d
ON CONFLICT (id_persona_rol) DO NOTHING;

INSERT INTO public.procedimiento (id_procedimiento,nombre,activo,observaciones)
VALUES ('de110006-0000-4000-8000-000000000001','Consulta general de demostración',true,'Procedimiento ficticio de desarrollo')
ON CONFLICT (id_procedimiento) DO NOTHING;

INSERT INTO public.procedimiento_paso (id_procedimiento_paso,id_procedimiento,nombre,indica_fin,id_rol)
SELECT ('de110007-0000-4000-8000-' || lpad(v.n::text,12,'0'))::uuid,'de110006-0000-4000-8000-000000000001',v.nombre,v.fin,r.id_rol
FROM (VALUES (1,'Toma de signos vitales',false),(2,'Evaluación médica',false),(3,'Indicaciones y cierre',true)) AS v(n,nombre,fin)
JOIN public.rol r ON r.codigo='DOCTOR'
ON CONFLICT (id_procedimiento_paso) DO NOTHING;

INSERT INTO public.procedimiento_paso_secuencia (id_procedimiento_paso_secuencia,id_procedimiento_paso,id_procedimiento_paso_referencia,tipo_secuencia)
SELECT ('de110008-0000-4000-8000-' || lpad(n::text,12,'0'))::uuid,
       ('de110007-0000-4000-8000-' || lpad(n::text,12,'0'))::uuid,
       ('de110007-0000-4000-8000-' || lpad((n+1)::text,12,'0'))::uuid,'SIGUIENTE'
FROM generate_series(1,2) n
ON CONFLICT (id_procedimiento_paso_secuencia) DO NOTHING;

-- Estado inicial deseado de las seis consultas (fuente única para carga y reinicio).
-- tipo: P pasada (3 pasos completados), H1 hoy a medio avanzar, N nueva (paso 1 pendiente).
CREATE TEMP TABLE demo_consulta ON COMMIT DROP AS
SELECT ('de110009-0000-4000-8000-' || lpad(v.n::text,12,'0'))::uuid AS id_consulta,
       ('de11000a-0000-4000-8000-' || lpad(v.n::text,12,'0'))::uuid AS id_cp,
       v.n, v.tipo,
       ('de110002-0000-4000-8000-' || lpad(v.paciente::text,12,'0'))::uuid AS paciente,
       ('de110002-0000-4000-8000-' || lpad(v.doctor::text,12,'0'))::uuid AS doctor,
       pg_temp.dia(v.dias, v.hora) AS inicio
FROM (VALUES
    (1,'P', 3,2,-20,'09:00'), (2,'H1',3,2,0,'08:00'), (3,'N',3,2,7,'10:00'),
    (4,'P', 4,5,-12,'14:00'), (5,'N', 4,5,0,'11:00'), (6,'N',4,5,3,'15:30')
) AS v(n,tipo,paciente,doctor,dias,hora);

CREATE TEMP TABLE demo_paso ON COMMIT DROP AS
SELECT ('de11000b-0000-4000-8000-' || lpad((c.n*10+p.paso)::text,12,'0'))::uuid AS id,
       c.id_cp, c.doctor, p.paso,
       ('de110007-0000-4000-8000-' || lpad(p.paso::text,12,'0'))::uuid AS id_paso_def,
       CASE WHEN c.tipo='P' OR (c.tipo='H1' AND p.paso=1) THEN 'COMPLETADO' ELSE 'PENDIENTE' END AS estado,
       CASE WHEN c.tipo='P' OR (c.tipo='H1' AND p.paso=1) THEN p.valor END AS valor,
       c.inicio + p.desde AS inicio,
       CASE WHEN c.tipo='P' OR (c.tipo='H1' AND p.paso=1) THEN c.inicio + p.hasta END AS fin
FROM demo_consulta c
JOIN (VALUES (1,'Demostración: PA 120/80, FC 72, T 36.6 °C',interval '5 minutes',interval '15 minutes'),
             (2,'Demostración: evaluación sin hallazgos',interval '15 minutes',interval '40 minutes'),
             (3,'Demostración: indicaciones entregadas',interval '40 minutes',interval '60 minutes')) AS p(paso,valor,desde,hasta)
  ON c.tipo='P' OR (c.tipo='H1' AND p.paso<=2) OR (c.tipo='N' AND p.paso=1);

CREATE TEMP TABLE demo_cierre ON COMMIT DROP AS
SELECT id_consulta, CASE WHEN tipo='P' THEN inicio + interval '60 minutes' END AS fin,
       CASE WHEN tipo='P' THEN 'Demostración: control sin complicaciones.' END AS observaciones
FROM demo_consulta;

-- Volver a anclar las fechas: desplaza consulta, procedimientos y pasos existentes (incluidos
-- los que haya creado la app) en la diferencia con el día deseado. No cambia estados.
CREATE TEMP TABLE demo_desfase ON COMMIT DROP AS
SELECT d.id_consulta, d.inicio - c.fecha_inicio AS desfase
FROM demo_consulta d JOIN public.consulta c ON c.id_consulta = d.id_consulta
WHERE c.fecha_inicio IS DISTINCT FROM d.inicio;

UPDATE public.consulta c SET fecha_inicio = c.fecha_inicio + x.desfase, fecha_fin = c.fecha_fin + x.desfase
FROM demo_desfase x WHERE c.id_consulta = x.id_consulta;
UPDATE public.consulta_procedimiento cp SET fecha_inicio = cp.fecha_inicio + x.desfase, fecha_fin = cp.fecha_fin + x.desfase
FROM demo_desfase x WHERE cp.id_consulta = x.id_consulta;
UPDATE public.consulta_procedimiento_paso p SET fecha_inicio = p.fecha_inicio + x.desfase, fecha_fin = p.fecha_fin + x.desfase
FROM public.consulta_procedimiento cp JOIN demo_desfase x ON x.id_consulta = cp.id_consulta
WHERE p.id_consulta_procedimiento = cp.id_consulta_procedimiento;

-- Insertar lo que falte con su estado inicial.
INSERT INTO public.consulta (id_consulta,fecha_inicio,fecha_fin,observaciones,id_persona_rol)
SELECT d.id_consulta, d.inicio, k.fin, k.observaciones, d.paciente
FROM demo_consulta d JOIN demo_cierre k USING (id_consulta)
ON CONFLICT (id_consulta) DO NOTHING;

INSERT INTO public.consulta_procedimiento (id_consulta_procedimiento,id_consulta,id_procedimiento,fecha_inicio,fecha_fin)
SELECT d.id_cp, d.id_consulta, 'de110006-0000-4000-8000-000000000001', d.inicio, k.fin
FROM demo_consulta d JOIN demo_cierre k USING (id_consulta)
ON CONFLICT (id_consulta_procedimiento) DO NOTHING;

INSERT INTO public.consulta_procedimiento_paso
    (id_consulta_procedimiento_paso,id_consulta_procedimiento,id_persona_rol,fecha_inicio,fecha_fin,estado,id_procedimiento_paso,valor)
SELECT id, id_cp, doctor, inicio, fin, estado, id_paso_def, valor FROM demo_paso
ON CONFLICT (id_consulta_procedimiento_paso) DO NOTHING;

-- Verificación estructural (los estados se comprueban en el modo reinicio).
DO $$ BEGIN
    IF (SELECT count(*) FROM public.consulta WHERE id_consulta::text LIKE 'de110009-%') <> 6
       OR (SELECT count(*) FROM public.persona_rol WHERE id_persona_rol IN ('de110002-0000-4000-8000-000000000004',
            'de110002-0000-4000-8000-000000000005','de110002-0000-4000-8000-000000000006')) <> 3
       OR (SELECT count(*) FROM public.procedimiento_paso_secuencia WHERE id_procedimiento_paso_secuencia::text LIKE 'de110008-%') <> 2 THEN
        RAISE EXCEPTION 'Los datos clínicos de demostración están incompletos; no se guardó ningún cambio.';
    END IF;
END $$;
COMMIT;
\echo 'Datos clínicos demo: 2 clínicas, 6 consultas de paciente.demo (pasadas, hoy y futuras).'
