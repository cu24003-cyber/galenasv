\set ON_ERROR_STOP on
-- Ejecutar después de aplicar 20261113_orden_examen_examen.sql. Todo se revierte.
BEGIN;

DO $$
DECLARE
    examen_prueba uuid := gen_random_uuid();
    paso uuid := gen_random_uuid();
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'orden_examen' AND column_name = 'id_examen'
                     AND data_type = 'uuid' AND is_nullable = 'YES') THEN
        RAISE EXCEPTION 'Falta orden_examen.id_examen (uuid, nullable)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'public.orden_examen'::regclass AND conname = 'fk_orden_examen_examen'
                     AND contype = 'f' AND confrelid = 'public.examen'::regclass
                     AND confdeltype = 'r' AND confupdtype = 'c') THEN
        RAISE EXCEPTION 'Falta fk_orden_examen_examen hacia examen con ON UPDATE CASCADE ON DELETE RESTRICT';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes
                   WHERE schemaname = 'public' AND tablename = 'orden_examen' AND indexname = 'ix_orden_examen_examen') THEN
        RAISE EXCEPTION 'Falta el índice ix_orden_examen_examen';
    END IF;

    INSERT INTO public.examen (id_examen, nombre, activo) VALUES (examen_prueba, 'Examen de prueba', true);
    INSERT INTO public.consulta_procedimiento_paso (id_consulta_procedimiento_paso, estado) VALUES (paso, 'PENDIENTE');

    -- Una orden anterior a la migración, sin examen, sigue siendo válida.
    INSERT INTO public.orden_examen (id_orden_examen, id_consulta_procedimiento_paso, indicaciones)
    VALUES (gen_random_uuid(), paso, 'Sin examen');
    INSERT INTO public.orden_examen (id_orden_examen, id_consulta_procedimiento_paso, id_examen)
    VALUES (gen_random_uuid(), paso, examen_prueba);

    BEGIN
        INSERT INTO public.orden_examen (id_orden_examen, id_consulta_procedimiento_paso, id_examen)
        VALUES (gen_random_uuid(), paso, gen_random_uuid());
        RAISE EXCEPTION 'Se aceptó un id_examen inexistente';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;

    BEGIN
        DELETE FROM public.examen WHERE id_examen = examen_prueba;
        RAISE EXCEPTION 'Se borró un examen con órdenes';
    -- ON DELETE RESTRICT informa restrict_violation (23001), no foreign_key_violation.
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
END $$;

ROLLBACK;
\echo 'Pruebas de orden_examen.id_examen correctas; datos de prueba revertidos.'
