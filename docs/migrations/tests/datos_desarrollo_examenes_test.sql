\set ON_ERROR_STOP on
-- Ejecutar después de aplicar 20261113_datos_desarrollo_examenes.sql. Solo lee.
-- Comprueba que "Evaluación médica" (de110007-…-2) tenga exactamente 3 exámenes asignables
-- (asociación y examen activos), y que el examen inactivo y la asociación inactiva existan
-- pero no cuenten.
DO $$
DECLARE
    asignables text;
BEGIN
    SELECT string_agg(e.nombre, ' | ' ORDER BY e.nombre) INTO asignables
      FROM public.procedimiento_paso_examen pe
      JOIN public.examen e ON e.id_examen = pe.id_examen
     WHERE pe.id_procedimiento_paso = 'de110007-0000-4000-8000-000000000002'
       AND pe.activo AND e.activo;
    IF asignables IS DISTINCT FROM 'Examen general de orina | Glucosa en ayunas | Hemograma completo' THEN
        RAISE EXCEPTION 'Asignables inesperados en Evaluación médica: %', asignables;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM public.procedimiento_paso_examen pe JOIN public.examen e ON e.id_examen = pe.id_examen
                   WHERE pe.id_procedimiento_paso = 'de110007-0000-4000-8000-000000000002' AND pe.activo AND NOT e.activo) THEN
        RAISE EXCEPTION 'Falta el examen inactivo asociado a Evaluación médica';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.procedimiento_paso_examen pe JOIN public.examen e ON e.id_examen = pe.id_examen
                   WHERE pe.id_procedimiento_paso = 'de110007-0000-4000-8000-000000000002' AND NOT pe.activo AND e.activo) THEN
        RAISE EXCEPTION 'Falta la asociación inactiva de Evaluación médica';
    END IF;
    IF (SELECT count(*) FROM public.examen_tipo_examen WHERE id_examen_tipo_examen::text LIKE 'de11000e-%') <> 5 THEN
        RAISE EXCEPTION 'Cada examen demo debe tener su clasificación';
    END IF;
END $$;
\echo 'Datos demo de exámenes correctos.'
