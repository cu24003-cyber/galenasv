\set ON_ERROR_STOP on
-- Ejecutar después de aplicar 20261112_documento_tipo_valor_unico.sql. Todo se revierte.
BEGIN;

DO $$
DECLARE
    persona_a uuid := gen_random_uuid();
    persona_b uuid := gen_random_uuid();
    tipo_a uuid := gen_random_uuid();
    tipo_b uuid := gen_random_uuid();
    -- Valor sintético; no corresponde a ninguna persona.
    numero text := 'prueba-' || gen_random_uuid()::text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'public.documento'::regclass AND conname = 'uq_documento_tipo_valor'
                     AND contype = 'u') THEN
        RAISE EXCEPTION 'Falta la restricción uq_documento_tipo_valor';
    END IF;

    INSERT INTO public.persona (id_persona) VALUES (persona_a), (persona_b);
    INSERT INTO public.tipo_documento (id_tipo_documento, nombre, activo)
    VALUES (tipo_a, 'Tipo A ' || numero, true), (tipo_b, 'Tipo B ' || numero, true);
    INSERT INTO public.documento (id_documento, id_persona, id_tipo_documento, valor)
    VALUES (gen_random_uuid(), persona_a, tipo_a, numero);

    BEGIN
        INSERT INTO public.documento (id_documento, id_persona, id_tipo_documento, valor)
        VALUES (gen_random_uuid(), persona_b, tipo_a, numero);
        RAISE EXCEPTION 'Se aceptó el mismo tipo y número para otra persona';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;

    -- El mismo número con otro tipo es un documento distinto.
    INSERT INTO public.documento (id_documento, id_persona, id_tipo_documento, valor)
    VALUES (gen_random_uuid(), persona_b, tipo_b, numero);
END $$;

ROLLBACK;
\echo 'Pruebas de unicidad de documento correctas; datos de prueba revertidos.'
