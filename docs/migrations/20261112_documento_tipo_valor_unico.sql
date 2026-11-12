-- PostgreSQL 13 o posterior. Aplicar después de 20261111_base_completa.sql.
-- psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261112_documento_tipo_valor_unico.sql
-- HU2: el login identifica a la persona por tipo y número de documento, por lo que
-- el par (id_tipo_documento, valor) debe ser único. Se puede ejecutar varias veces.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE public.documento IN SHARE ROW EXCLUSIVE MODE;

-- Detecta duplicados antes de crear la restricción. Solo se informan los id_documento,
-- nunca los valores, porque son datos personales.
DO $$
DECLARE
    grupos integer;
    ids text;
BEGIN
    SELECT count(*), string_agg(documentos, '; ')
      INTO grupos, ids
      FROM (SELECT string_agg(id_documento::text, ', ' ORDER BY id_documento) AS documentos
              FROM public.documento
             WHERE id_tipo_documento IS NOT NULL AND valor IS NOT NULL
             GROUP BY id_tipo_documento, valor
            HAVING count(*) > 1) d;
    IF grupos > 0 THEN
        RAISE EXCEPTION 'Hay % grupos de documentos con el mismo tipo y valor. Corríjalos antes de aplicar la migración. id_documento: %', grupos, ids;
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'public.documento'::regclass AND conname = 'uq_documento_tipo_valor') THEN
        ALTER TABLE public.documento
            ADD CONSTRAINT uq_documento_tipo_valor UNIQUE (id_tipo_documento, valor);
    END IF;
END $$;

COMMENT ON CONSTRAINT uq_documento_tipo_valor ON public.documento IS
    'Un número de documento identifica a una sola persona dentro de su tipo (login HU2).';
COMMIT;
