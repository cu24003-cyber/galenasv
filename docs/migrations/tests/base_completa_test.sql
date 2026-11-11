\set ON_ERROR_STOP on
BEGIN;
DO $$
DECLARE faltantes text;
BEGIN
    SELECT string_agg(esperado.tabla || '.' || columna, ', ') INTO faltantes
    FROM (VALUES
        ('clinica', ARRAY['id_clinica','nombre','activo','tipo','comentarios']),
        ('consulta', ARRAY['id_consulta','fecha_inicio','fecha_fin','referencia_externa','observaciones','id_persona_rol']),
        ('consulta_procedimiento', ARRAY['id_consulta_procedimiento','id_procedimiento','fecha_inicio','fecha_fin','observaciones','id_consulta']),
        ('consulta_procedimiento_paso', ARRAY['id_consulta_procedimiento_paso','fecha_inicio','fecha_fin','estado','valor','id_consulta_procedimiento','id_persona_rol','id_procedimiento_paso']),
        ('documento', ARRAY['id_documento','valor','ruta_fisica','id_persona','id_tipo_documento']),
        ('examen', ARRAY['id_examen','nombre','activo','observaciones']),
        ('examen_resultado', ARRAY['id_examen_resultado','fecha_creacion','resultado','interpretacion','ruta_atestado','id_orden_examen']),
        ('examen_tipo_examen', ARRAY['id_examen_tipo_examen','fecha_creacion','observaciones','id_examen','id_tipo_examen']),
        ('medio_contacto', ARRAY['id_medio_contacto','valor','fecha_creacion','id_persona','id_tipo_medio_contacto']),
        ('orden_examen', ARRAY['id_orden_examen','fecha_creacion','indicaciones','id_consulta_procedimiento_paso']),
        ('persona', ARRAY['id_persona','nombres','apellidos','fecha_nacimiento','fecha_creacion']),
        ('persona_rol', ARRAY['id_persona_rol','fecha_creacion','id_clinica','id_persona','id_rol']),
        ('procedimiento', ARRAY['id_procedimiento','nombre','activo','observaciones']),
        ('procedimiento_paso', ARRAY['id_procedimiento_paso','nombre','indica_fin','id_procedimiento','id_rol']),
        ('procedimiento_paso_examen', ARRAY['id_procedimiento_paso_examen','fecha_creacion','activo','observaciones','id_examen','id_procedimiento_paso']),
        ('procedimiento_paso_secuencia', ARRAY['id_procedimiento_paso_secuencia','id_procedimiento_paso_referencia','tipo_secuencia','id_procedimiento_paso']),
        ('rol', ARRAY['id_rol','nombre','codigo','activo','observaciones']),
        ('tipo_documento', ARRAY['id_tipo_documento','nombre','indicaciones','expresion_regular','activo']),
        ('tipo_examen', ARRAY['id_tipo_examen','nombre','activo','observaciones']),
        ('tipo_medio_contacto', ARRAY['id_tipo_medio_contacto','nombre','indicaciones','expresion_regular','activo']),
        ('usuario', ARRAY['id_usuario','id_persona','nombre_usuario','correo','password_hash','activo','fecha_creacion'])
    ) AS esperado(tabla, columnas)
    CROSS JOIN LATERAL unnest(esperado.columnas) AS columna
    WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns c
                      WHERE c.table_schema='public' AND c.table_name=esperado.tabla AND c.column_name=columna);
    IF faltantes IS NOT NULL THEN RAISE EXCEPTION 'Faltan columnas del backend: %', faltantes; END IF;
    IF (SELECT count(*) FROM public.rol WHERE codigo IN ('ADMINISTRACION','DOCTOR','PACIENTE')) <> 3 THEN
        RAISE EXCEPTION 'Faltan los códigos de los tres perfiles.';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype='f' AND NOT convalidated) THEN
        RAISE EXCEPTION 'Hay claves foráneas sin validar.';
    END IF;
END $$;

-- Un recorrido clínico completo con datos sintéticos; se revierte al terminar.
DO $$
DECLARE
    id uuid := gen_random_uuid();
    medico uuid := gen_random_uuid();
    inexistente uuid := gen_random_uuid();
BEGIN
    INSERT INTO public.clinica (id_clinica,nombre,activo) VALUES(id,'Clínica de prueba SQL',true);
    INSERT INTO public.persona (id_persona,nombres,apellidos,fecha_nacimiento) VALUES(id,'María José','Muñoz','2000-01-01');
    INSERT INTO public.persona_rol (id_persona_rol,id_persona,id_rol,id_clinica)
        SELECT id,id,id_rol,id FROM public.rol WHERE codigo='PACIENTE';
    INSERT INTO public.persona_rol (id_persona_rol,id_persona,id_rol,id_clinica)
        SELECT medico,id,id_rol,id FROM public.rol WHERE codigo='DOCTOR';
    INSERT INTO public.tipo_documento (id_tipo_documento,nombre,activo,expresion_regular) VALUES(id,'Documento de prueba',true,'[0-9]+');
    INSERT INTO public.documento (id_documento,id_persona,id_tipo_documento,valor) VALUES(id,id,id,'123456');
    INSERT INTO public.tipo_medio_contacto (id_tipo_medio_contacto,nombre,activo) VALUES(id,'Contacto de prueba',true);
    INSERT INTO public.medio_contacto (id_medio_contacto,id_persona,id_tipo_medio_contacto,valor) VALUES(id,id,id,'prueba@example.invalid');
    INSERT INTO public.procedimiento (id_procedimiento,nombre,activo) VALUES(id,'Procedimiento de prueba',true);
    INSERT INTO public.procedimiento_paso (id_procedimiento_paso,id_procedimiento,nombre,indica_fin,id_rol)
        SELECT id,id,'Paso de prueba',true,id_rol FROM public.rol WHERE codigo='DOCTOR';
    INSERT INTO public.procedimiento_paso_secuencia (id_procedimiento_paso_secuencia,id_procedimiento_paso,id_procedimiento_paso_referencia,tipo_secuencia)
        VALUES(id,id,id,'SIGUIENTE');
    INSERT INTO public.tipo_examen (id_tipo_examen,nombre,activo) VALUES(id,'Tipo de prueba',true);
    INSERT INTO public.examen (id_examen,nombre,activo) VALUES(id,'Examen de prueba',true);
    INSERT INTO public.examen_tipo_examen (id_examen_tipo_examen,id_examen,id_tipo_examen) VALUES(id,id,id);
    INSERT INTO public.procedimiento_paso_examen (id_procedimiento_paso_examen,id_procedimiento_paso,id_examen) VALUES(id,id,id);
    INSERT INTO public.consulta (id_consulta,id_persona_rol,observaciones) VALUES(id,id,'Consulta de prueba');
    INSERT INTO public.consulta_procedimiento (id_consulta_procedimiento,id_consulta,id_procedimiento,fecha_inicio) VALUES(id,id,id,CURRENT_TIMESTAMP);
    INSERT INTO public.consulta_procedimiento_paso (id_consulta_procedimiento_paso,id_consulta_procedimiento,id_persona_rol,id_procedimiento_paso,estado,valor)
        VALUES(id,id,medico,id,'FINALIZADO','95 mg/dL');
    INSERT INTO public.orden_examen (id_orden_examen,id_consulta_procedimiento_paso,indicaciones) VALUES(id,id,'Orden de prueba');
    INSERT INTO public.examen_resultado (id_examen_resultado,id_orden_examen,resultado) VALUES(id,id,'95 mg/dL');
    BEGIN
        INSERT INTO public.consulta_procedimiento_paso (id_consulta_procedimiento_paso,id_procedimiento_paso) VALUES(inexistente,inexistente);
        RAISE EXCEPTION 'Se admitió un paso de procedimiento inexistente.';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO public.procedimiento_paso_secuencia (id_procedimiento_paso_secuencia,id_procedimiento_paso_referencia) VALUES(inexistente,inexistente);
        RAISE EXCEPTION 'Se admitió una secuencia hacia un paso inexistente.';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
END $$;
ROLLBACK;
\echo 'Estructura y flujo clínico completos: correctos; datos de prueba revertidos.'
