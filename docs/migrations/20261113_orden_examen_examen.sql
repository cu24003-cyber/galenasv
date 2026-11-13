-- PostgreSQL 13 o posterior. Aplicar después de 20261111_base_completa.sql.
-- psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261113_orden_examen_examen.sql
-- HU4: una orden de examen indica qué examen se ordenó. La columna es nullable porque las
-- órdenes anteriores no tienen examen; la obligatoriedad la impone la API REST al crear la
-- orden. Se puede ejecutar varias veces.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE public.orden_examen ADD COLUMN IF NOT EXISTS id_examen uuid;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'public.orden_examen'::regclass AND conname = 'fk_orden_examen_examen') THEN
        ALTER TABLE public.orden_examen
            ADD CONSTRAINT fk_orden_examen_examen FOREIGN KEY (id_examen)
            REFERENCES public.examen (id_examen) ON UPDATE CASCADE ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_orden_examen_examen ON public.orden_examen (id_examen);

COMMENT ON COLUMN public.orden_examen.id_examen IS
    'Examen ordenado (HU4). Null en órdenes anteriores a esta migración.';
COMMIT;
