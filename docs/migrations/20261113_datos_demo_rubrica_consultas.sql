\set ON_ERROR_STOP on
-- Opcional, solo para desarrollo y revisión: las 11 consultas demo del guion (docs/datos-demo-rubrica.md §9).
-- Requiere 20261113_datos_demo_rubrica.sql (clínicas, procedimientos, exámenes y asignaciones).
-- psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261113_datos_demo_rubrica_consultas.sql
--
-- UUID fijos: de120010 consultas (1-11)   de120011 consulta_procedimiento (1-11)
--             de120012 pasos ejecutados (consulta*10 + orden del paso)   de120013 órdenes (1-2)
-- Consultas: 1 SA-1 … 8 SA-8 (Clínica Santa Ana), 9 SS-1, 10 SS-2, 11 SS-3 (Clínica San Salvador).
-- Fechas relativas al día local de El Salvador. Al repetirlo inserta lo que falte y vuelve a anclar
-- las fechas al día de hoy sin cambiar estados. Con -v reiniciar=1 (scripts/reiniciar-demo.sh)
-- además devuelve las 11 consultas a su estado inicial. Todos los textos son inventados.
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$ BEGIN
    IF (SELECT count(*) FROM public.persona_rol WHERE id_persona_rol::text LIKE 'de12000e-%') <> 11
       OR (SELECT count(*) FROM public.procedimiento_paso WHERE id_procedimiento_paso::text LIKE 'de120008-%') <> 6 THEN
        RAISE EXCEPTION 'Faltan los datos demo del guion. Aplique primero 20261113_datos_demo_rubrica.sql.';
    END IF;
END $$;

-- Instantes relativos al día local de El Salvador: dia(días desde hoy, hora local).
CREATE TEMP TABLE demo_tiempo (base timestamp) ON COMMIT DROP;
INSERT INTO demo_tiempo VALUES (date_trunc('day', now() AT TIME ZONE 'America/El_Salvador'));
CREATE FUNCTION pg_temp.dia(dias integer, hora text) RETURNS timestamptz LANGUAGE sql STABLE AS
$$ SELECT (base + dias * interval '1 day' + hora::interval) AT TIME ZONE 'America/El_Salvador' FROM demo_tiempo $$;
CREATE FUNCTION pg_temp.uid(prefijo text, n integer) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT (prefijo || '-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid $$;

-- Estado inicial deseado (fuente única para carga y reinicio).
-- paciente: número de persona_rol (de12000e). proc: 1 Consulta general, 2 Control de hipertensión.
-- hechos: pasos COMPLETADOS (0 a 3). Si hechos < 3 existe además el siguiente paso PENDIENTE.
-- r1, r2, r3: persona_rol responsable de cada paso (de12000e).
CREATE TEMP TABLE demo_consulta ON COMMIT DROP AS
SELECT pg_temp.uid('de120010', v.n) AS id_consulta, pg_temp.uid('de120011', v.n) AS id_cp,
       v.n, v.codigo, pg_temp.uid('de12000e', v.paciente) AS paciente, v.proc, v.hechos,
       ARRAY[pg_temp.uid('de12000e', v.r1), pg_temp.uid('de12000e', v.r2), pg_temp.uid('de12000e', v.r3)] AS responsables,
       pg_temp.dia(v.dias, v.hora) AS inicio
FROM (VALUES
    -- Santa Ana: Carlos 1, Lucía 3, Marta 4; pacientes Ana 6, Julia 10, Juan 11.
    (1, 'SA-1', 6, 1, 3, 3, 1, 4, -20, '09:00'),
    (2, 'SA-2', 10, 2, 3, 3, 1, 4, -1, '15:00'),
    (3, 'SA-3', 6, 1, 1, 3, 1, 4, 0, '08:00'),
    (4, 'SA-4', 10, 2, 0, 3, 1, 4, 0, '09:15'),
    (5, 'SA-5', 11, 1, 0, 3, 1, 4, 0, '10:30'),
    (6, 'SA-6', 6, 1, 2, 3, 1, 4, 0, '11:45'),
    (7, 'SA-7', 11, 1, 0, 3, 1, 4, 3, '08:30'),
    (8, 'SA-8', 6, 2, 0, 3, 1, 4, 10, '10:00'),
    -- San Salvador: Carlos 2, Marta 5, Rosa 8, Jorge 9; paciente Ana 7.
    (9, 'SS-1', 7, 1, 3, 8, 9, 5, -10, '14:00'),
    (10, 'SS-2', 7, 1, 1, 8, 2, 5, 0, '13:00'),
    (11, 'SS-3', 7, 2, 0, 8, 2, 5, 5, '09:00')
) AS v(n, codigo, paciente, proc, hechos, r1, r2, r3, dias, hora);

CREATE TEMP TABLE demo_paso ON COMMIT DROP AS
SELECT pg_temp.uid('de120012', c.n * 10 + p.orden) AS id, c.id_cp, c.responsables[p.orden] AS responsable, p.orden,
       pg_temp.uid('de120008', (c.proc - 1) * 3 + p.orden) AS id_paso_def,
       CASE WHEN p.orden <= c.hechos THEN 'COMPLETADO' ELSE 'PENDIENTE' END AS estado,
       CASE WHEN p.orden <= c.hechos THEN p.valor END AS valor,
       c.inicio + p.desde AS inicio,
       CASE WHEN p.orden <= c.hechos THEN c.inicio + p.hasta END AS fin
FROM demo_consulta c
JOIN (VALUES (1, 'PA 120/80, FC 72, T 36.6 °C, peso 62 kg', interval '5 minutes', interval '15 minutes'),
             (2, 'Paciente estable. Se indican exámenes de control.', interval '15 minutes', interval '40 minutes'),
             (3, 'Se entregan indicaciones y cita de seguimiento.', interval '40 minutes', interval '60 minutes'))
     AS p(orden, valor, desde, hasta)
  ON p.orden <= least(c.hechos + 1, 3);

CREATE TEMP TABLE demo_cierre ON COMMIT DROP AS
SELECT id_consulta, CASE WHEN hechos = 3 THEN inicio + interval '60 minutes' END AS fin,
       CASE WHEN hechos = 3 THEN 'Consulta cerrada sin complicaciones.' END AS observaciones
FROM demo_consulta;

-- Órdenes de examen: Hemograma completo en la Consulta médica de SA-1 y Glucosa en ayunas en la de SA-6.
CREATE TEMP TABLE demo_orden ON COMMIT DROP AS
SELECT pg_temp.uid('de120013', o.n) AS id, pg_temp.uid('de120012', o.consulta * 10 + 2) AS id_paso,
       pg_temp.uid('de120005', o.examen) AS id_examen, c.inicio + interval '30 minutes' AS creacion, o.indicaciones
FROM (VALUES (1, 1, 1, 'Control de rutina.'), (2, 6, 2, 'Ayuno de 8 horas.')) AS o(n, consulta, examen, indicaciones)
JOIN demo_consulta c ON c.n = o.consulta;

\if :{?reiniciar}
-- MODO REINICIO (psql -v reiniciar=1 o scripts/reiniciar-demo.sh). Borra solo lo que cuelga de las
-- 11 consultas demo y no forma parte de su estado inicial: resultados, órdenes agregadas, pasos y
-- procedimientos agregados desde la app. Nada fuera de esas consultas se toca.
CREATE TEMP TABLE demo_cp_actual ON COMMIT DROP AS
SELECT cp.id_consulta_procedimiento FROM public.consulta_procedimiento cp
WHERE cp.id_consulta IN (SELECT id_consulta FROM demo_consulta);
CREATE TEMP TABLE demo_paso_actual ON COMMIT DROP AS
SELECT x.id_consulta_procedimiento_paso FROM public.consulta_procedimiento_paso x
WHERE x.id_consulta_procedimiento IN (SELECT id_consulta_procedimiento FROM demo_cp_actual);

DELETE FROM public.examen_resultado WHERE id_orden_examen IN (
    SELECT id_orden_examen FROM public.orden_examen
    WHERE id_consulta_procedimiento_paso IN (SELECT id_consulta_procedimiento_paso FROM demo_paso_actual));
DELETE FROM public.orden_examen
WHERE id_consulta_procedimiento_paso IN (SELECT id_consulta_procedimiento_paso FROM demo_paso_actual)
  AND id_orden_examen NOT IN (SELECT id FROM demo_orden);
DELETE FROM public.consulta_procedimiento_paso
WHERE id_consulta_procedimiento_paso IN (SELECT id_consulta_procedimiento_paso FROM demo_paso_actual)
  AND id_consulta_procedimiento_paso NOT IN (SELECT id FROM demo_paso);
DELETE FROM public.consulta_procedimiento
WHERE id_consulta_procedimiento IN (SELECT id_consulta_procedimiento FROM demo_cp_actual)
  AND id_consulta_procedimiento NOT IN (SELECT id_cp FROM demo_consulta);

-- Restaura todos los campos de las filas fijas.
INSERT INTO public.consulta (id_consulta, fecha_inicio, fecha_fin, referencia_externa, observaciones, id_persona_rol)
SELECT d.id_consulta, d.inicio, k.fin, NULL, k.observaciones, d.paciente
FROM demo_consulta d JOIN demo_cierre k USING (id_consulta)
ON CONFLICT (id_consulta) DO UPDATE SET fecha_inicio = EXCLUDED.fecha_inicio, fecha_fin = EXCLUDED.fecha_fin,
    referencia_externa = NULL, observaciones = EXCLUDED.observaciones, id_persona_rol = EXCLUDED.id_persona_rol;
INSERT INTO public.consulta_procedimiento (id_consulta_procedimiento, id_consulta, id_procedimiento, fecha_inicio, fecha_fin, observaciones)
SELECT d.id_cp, d.id_consulta, pg_temp.uid('de120007', d.proc), d.inicio, k.fin, NULL
FROM demo_consulta d JOIN demo_cierre k USING (id_consulta)
ON CONFLICT (id_consulta_procedimiento) DO UPDATE SET id_consulta = EXCLUDED.id_consulta,
    id_procedimiento = EXCLUDED.id_procedimiento, fecha_inicio = EXCLUDED.fecha_inicio, fecha_fin = EXCLUDED.fecha_fin,
    observaciones = NULL;
INSERT INTO public.consulta_procedimiento_paso
    (id_consulta_procedimiento_paso, id_consulta_procedimiento, id_persona_rol, fecha_inicio, fecha_fin, estado, id_procedimiento_paso, valor)
SELECT id, id_cp, responsable, inicio, fin, estado, id_paso_def, valor FROM demo_paso
ON CONFLICT (id_consulta_procedimiento_paso) DO UPDATE SET id_consulta_procedimiento = EXCLUDED.id_consulta_procedimiento,
    id_persona_rol = EXCLUDED.id_persona_rol, fecha_inicio = EXCLUDED.fecha_inicio, fecha_fin = EXCLUDED.fecha_fin,
    estado = EXCLUDED.estado, id_procedimiento_paso = EXCLUDED.id_procedimiento_paso, valor = EXCLUDED.valor;
INSERT INTO public.orden_examen (id_orden_examen, id_consulta_procedimiento_paso, fecha_creacion, indicaciones, id_examen)
SELECT id, id_paso, creacion, indicaciones, id_examen FROM demo_orden
ON CONFLICT (id_orden_examen) DO UPDATE SET id_consulta_procedimiento_paso = EXCLUDED.id_consulta_procedimiento_paso,
    fecha_creacion = EXCLUDED.fecha_creacion, indicaciones = EXCLUDED.indicaciones, id_examen = EXCLUDED.id_examen;

DO $$ BEGIN
    IF (SELECT count(*) FROM public.consulta_procedimiento_paso x JOIN public.consulta_procedimiento cp USING (id_consulta_procedimiento)
        WHERE cp.id_consulta IN (SELECT id_consulta FROM demo_consulta)) <> (SELECT count(*) FROM demo_paso)
       OR (SELECT count(*) FROM public.orden_examen o JOIN public.consulta_procedimiento_paso x USING (id_consulta_procedimiento_paso)
           JOIN public.consulta_procedimiento cp USING (id_consulta_procedimiento)
           WHERE cp.id_consulta IN (SELECT id_consulta FROM demo_consulta)) <> (SELECT count(*) FROM demo_orden) THEN
        RAISE EXCEPTION 'El reinicio no dejó exactamente los pasos y órdenes iniciales; no se guardó ningún cambio.';
    END IF;
END $$;
\echo 'Modo reinicio: las 11 consultas demo volvieron a su estado inicial.'
\endif

-- Volver a anclar las fechas: desplaza consulta, procedimientos, pasos y órdenes existentes
-- (incluidos los que haya creado la app) en la diferencia con el día deseado. No cambia estados.
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
UPDATE public.orden_examen o SET fecha_creacion = o.fecha_creacion + x.desfase
FROM public.consulta_procedimiento_paso p
JOIN public.consulta_procedimiento cp ON cp.id_consulta_procedimiento = p.id_consulta_procedimiento
JOIN demo_desfase x ON x.id_consulta = cp.id_consulta
WHERE o.id_consulta_procedimiento_paso = p.id_consulta_procedimiento_paso;

-- Insertar lo que falte con su estado inicial.
INSERT INTO public.consulta (id_consulta, fecha_inicio, fecha_fin, observaciones, id_persona_rol)
SELECT d.id_consulta, d.inicio, k.fin, k.observaciones, d.paciente
FROM demo_consulta d JOIN demo_cierre k USING (id_consulta)
ON CONFLICT (id_consulta) DO NOTHING;

INSERT INTO public.consulta_procedimiento (id_consulta_procedimiento, id_consulta, id_procedimiento, fecha_inicio, fecha_fin)
SELECT d.id_cp, d.id_consulta, pg_temp.uid('de120007', d.proc), d.inicio, k.fin
FROM demo_consulta d JOIN demo_cierre k USING (id_consulta)
ON CONFLICT (id_consulta_procedimiento) DO NOTHING;

INSERT INTO public.consulta_procedimiento_paso
    (id_consulta_procedimiento_paso, id_consulta_procedimiento, id_persona_rol, fecha_inicio, fecha_fin, estado, id_procedimiento_paso, valor)
SELECT id, id_cp, responsable, inicio, fin, estado, id_paso_def, valor FROM demo_paso
ON CONFLICT (id_consulta_procedimiento_paso) DO NOTHING;

-- Una orden demo solo se inserta si su paso existe (la app pudo haber retrocedido o cambiado la consulta).
INSERT INTO public.orden_examen (id_orden_examen, id_consulta_procedimiento_paso, fecha_creacion, indicaciones, id_examen)
SELECT o.id, o.id_paso, o.creacion, o.indicaciones, o.id_examen FROM demo_orden o
WHERE EXISTS (SELECT 1 FROM public.consulta_procedimiento_paso x WHERE x.id_consulta_procedimiento_paso = o.id_paso)
ON CONFLICT (id_orden_examen) DO NOTHING;

-- Verificación estructural (los estados se comprueban en el modo reinicio y en la prueba SQL).
DO $$ BEGIN
    IF (SELECT count(*) FROM public.consulta WHERE id_consulta::text LIKE 'de120010-%') <> 11
       OR (SELECT count(*) FROM public.consulta_procedimiento WHERE id_consulta_procedimiento::text LIKE 'de120011-%') <> 11 THEN
        RAISE EXCEPTION 'Las consultas demo del guion están incompletas; no se guardó ningún cambio.';
    END IF;
END $$;
COMMIT;
\echo 'Consultas demo del guion: 8 en Clínica Santa Ana y 3 en Clínica San Salvador (pasadas, hoy y futuras).'
