\set ON_ERROR_STOP on
-- Opcional: catálogo demo de exámenes para la HU4. Solo para desarrollo.
-- Requiere 20261112_datos_desarrollo_clinicos.sql (procedimiento y pasos demo).
-- psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261113_datos_desarrollo_examenes.sql
--
-- Solo toca filas con UUID fijos de demostración:
--   de11000c exámenes (1-5)            de11000d tipo de examen (1)
--   de11000e clasificaciones (1-5)     de11000f asociaciones con "Evaluación médica" (1-5)
-- Asignables a "Evaluación médica" (de110007-…-2): los exámenes 1, 2 y 3. El 4 tiene la
-- asociación inactiva y el 5 está inactivo; existen para probar el filtro. Es solo catálogo:
-- no toca consultas, pasos ni órdenes. Al repetirlo no cambia nada (ON CONFLICT DO NOTHING).
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.procedimiento_paso
                   WHERE id_procedimiento_paso = 'de110007-0000-4000-8000-000000000002') THEN
        RAISE EXCEPTION 'Falta el procedimiento demo. Aplique primero 20261112_datos_desarrollo_clinicos.sql.';
    END IF;
END $$;

INSERT INTO public.tipo_examen (id_tipo_examen, nombre, activo, observaciones)
VALUES ('de11000d-0000-4000-8000-000000000001', 'Laboratorio clínico (demo)', true, 'Tipo de examen de demostración.')
ON CONFLICT (id_tipo_examen) DO NOTHING;

INSERT INTO public.examen (id_examen, nombre, activo, observaciones)
SELECT ('de11000c-0000-4000-8000-' || lpad(v.n::text, 12, '0'))::uuid, v.nombre, v.activo, 'Examen de demostración.'
FROM (VALUES (1, 'Hemograma completo', true),
             (2, 'Glucosa en ayunas', true),
             (3, 'Examen general de orina', true),
             (4, 'Perfil lipídico', true),
             (5, 'Examen descontinuado (demo)', false)) AS v(n, nombre, activo)
ON CONFLICT (id_examen) DO NOTHING;

INSERT INTO public.examen_tipo_examen (id_examen_tipo_examen, id_examen, id_tipo_examen)
SELECT ('de11000e-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
       ('de11000c-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
       'de11000d-0000-4000-8000-000000000001'
FROM generate_series(1, 5) n
ON CONFLICT (id_examen_tipo_examen) DO NOTHING;

INSERT INTO public.procedimiento_paso_examen (id_procedimiento_paso_examen, id_procedimiento_paso, id_examen, activo, observaciones)
SELECT ('de11000f-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
       'de110007-0000-4000-8000-000000000002',
       ('de11000c-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
       n <> 4, 'Asociación de demostración.'
FROM generate_series(1, 5) n
ON CONFLICT (id_procedimiento_paso_examen) DO NOTHING;

COMMIT;
