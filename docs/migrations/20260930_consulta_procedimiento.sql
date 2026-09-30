-- Ejecutar antes de desplegar el WAR, en la base de GalenoSV.
-- Es transaccional: los registros huérfanos provocan rollback; no se eliminan datos.
BEGIN;
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT * FROM (VALUES
    ('consulta','fecha_inicio'), ('consulta','fecha_fin'),
    ('consulta_procedimiento','fecha_inicio'), ('consulta_procedimiento','fecha_fin'),
    ('consulta_procedimiento_paso','fecha_inicio'), ('consulta_procedimiento_paso','fecha_fin'),
    ('orden_examen','fecha_creacion'), ('procedimiento_paso_examen','fecha_creacion'),
    ('examen_tipo_examen','fecha_creacion')) AS fechas(tabla,columna)
  LOOP
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema()
      AND table_name=r.tabla AND column_name=r.columna AND data_type='timestamp with time zone') THEN
      RAISE EXCEPTION 'Se esperaba timestamptz en %.%. Revise la zona de los datos antes de convertirlos.',r.tabla,r.columna;
    END IF;
  END LOOP;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='consulta_procedimiento'::regclass
    AND confrelid='procedimiento'::regclass AND contype='f'
    AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='consulta_procedimiento'::regclass AND attname='id_procedimiento')]) THEN
    ALTER TABLE consulta_procedimiento ADD CONSTRAINT fk_consulta_procedimiento_catalogo
      FOREIGN KEY (id_procedimiento) REFERENCES procedimiento(id_procedimiento);
  END IF;
END $$;
ALTER TABLE consulta_procedimiento_paso ADD COLUMN IF NOT EXISTS id_procedimiento_paso uuid;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='consulta_procedimiento_paso'::regclass
    AND confrelid='procedimiento_paso'::regclass AND contype='f') THEN
    ALTER TABLE consulta_procedimiento_paso ADD CONSTRAINT fk_consulta_paso_catalogo
      FOREIGN KEY (id_procedimiento_paso) REFERENCES procedimiento_paso(id_procedimiento_paso);
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_consulta_abierta_paciente ON consulta(id_persona_rol) WHERE fecha_fin IS NULL;
CREATE INDEX IF NOT EXISTS idx_consulta_procedimiento_consulta ON consulta_procedimiento(id_consulta);
CREATE INDEX IF NOT EXISTS idx_consulta_paso_procedimiento ON consulta_procedimiento_paso(id_consulta_procedimiento);
COMMIT;
