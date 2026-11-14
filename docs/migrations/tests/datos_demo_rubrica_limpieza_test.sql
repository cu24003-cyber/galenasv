\set ON_ERROR_STOP on
-- Ejecutar después de 20261113_limpieza_demo_anterior.sql. Solo lee; no modifica datos.
-- Comprueba que no quede ninguna fila de la demo anterior (UUID de110000… a de11000f, salvo el
-- tipo de documento DUI de110004, que se conserva) ni cuentas admin.demo, doctor.demo o paciente.demo.
DO $$
DECLARE
    restos text := concat_ws(', ',
        (SELECT nullif(count(*), 0) || ' clínica(s)' FROM public.clinica WHERE id_clinica::text LIKE 'de110000-%'),
        (SELECT nullif(count(*), 0) || ' persona(s)' FROM public.persona WHERE id_persona::text LIKE 'de110001-%'),
        (SELECT nullif(count(*), 0) || ' asignación(es)' FROM public.persona_rol WHERE id_persona_rol::text LIKE 'de110002-%'),
        (SELECT nullif(count(*), 0) || ' cuenta(s)' FROM public.usuario
          WHERE id_usuario::text LIKE 'de110003-%' OR lower(nombre_usuario) IN ('admin.demo', 'doctor.demo', 'paciente.demo')),
        (SELECT nullif(count(*), 0) || ' documento(s)' FROM public.documento WHERE id_documento::text LIKE 'de110005-%'),
        (SELECT nullif(count(*), 0) || ' procedimiento(s)' FROM public.procedimiento WHERE id_procedimiento::text LIKE 'de110006-%'),
        (SELECT nullif(count(*), 0) || ' paso(s) de procedimiento' FROM public.procedimiento_paso WHERE id_procedimiento_paso::text LIKE 'de110007-%'),
        (SELECT nullif(count(*), 0) || ' secuencia(s)' FROM public.procedimiento_paso_secuencia
          WHERE id_procedimiento_paso_secuencia::text LIKE 'de110008-%'),
        (SELECT nullif(count(*), 0) || ' consulta(s)' FROM public.consulta WHERE id_consulta::text LIKE 'de110009-%'),
        (SELECT nullif(count(*), 0) || ' procedimiento(s) de consulta' FROM public.consulta_procedimiento
          WHERE id_consulta_procedimiento::text LIKE 'de11000a-%'),
        (SELECT nullif(count(*), 0) || ' paso(s) de consulta' FROM public.consulta_procedimiento_paso
          WHERE id_consulta_procedimiento_paso::text LIKE 'de11000b-%'),
        (SELECT nullif(count(*), 0) || ' examen(es)' FROM public.examen WHERE id_examen::text LIKE 'de11000c-%'),
        (SELECT nullif(count(*), 0) || ' tipo(s) de examen' FROM public.tipo_examen WHERE id_tipo_examen::text LIKE 'de11000d-%'),
        (SELECT nullif(count(*), 0) || ' clasificación(es)' FROM public.examen_tipo_examen WHERE id_examen_tipo_examen::text LIKE 'de11000e-%'),
        (SELECT nullif(count(*), 0) || ' examen(es) asignable(s)' FROM public.procedimiento_paso_examen
          WHERE id_procedimiento_paso_examen::text LIKE 'de11000f-%'));
BEGIN
    IF restos <> '' THEN
        RAISE EXCEPTION 'Quedan filas de la demo anterior: %', restos;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.tipo_documento WHERE lower(btrim(nombre)) = 'dui') THEN
        RAISE EXCEPTION 'La limpieza no debe borrar el tipo de documento DUI';
    END IF;
END $$;
\echo 'Demo anterior eliminada.'
