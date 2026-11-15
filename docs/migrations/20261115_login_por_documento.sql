BEGIN;
SET LOCAL lock_timeout = '5s';

-- Las sesiones se resuelven desde documento -> persona; las asignaciones siguen en persona_rol.
DROP TABLE IF EXISTS public.usuario;

COMMIT;
