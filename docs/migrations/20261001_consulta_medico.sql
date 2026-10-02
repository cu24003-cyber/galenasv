-- Ejecutar después de 20260930_consulta_procedimiento.sql y antes del nuevo WAR.
-- Las consultas históricas quedan sin médico hasta que un administrador verifique
-- quién las realizó. No se infiere la autoría a partir de los pasos.
BEGIN;
ALTER TABLE consulta ADD COLUMN IF NOT EXISTS id_medico_rol uuid;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='consulta'::regclass
    AND conname='fk_consulta_medico_rol') THEN
    ALTER TABLE consulta ADD CONSTRAINT fk_consulta_medico_rol
      FOREIGN KEY (id_medico_rol) REFERENCES persona_rol(id_persona_rol);
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_consulta_medico_clinica
  ON consulta(id_medico_rol, fecha_inicio DESC);
COMMIT;

-- Tras verificar cada caso histórico, asignar id_medico_rol con una fila de
-- persona_rol cuyo rol sea Médico y cuya clínica sea la del paciente.
-- Las consultas sin asignar no serán visibles en el historial.
