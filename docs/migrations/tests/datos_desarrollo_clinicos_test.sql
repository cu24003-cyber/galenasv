\set ON_ERROR_STOP on
-- Ejecutar después de 20261112_datos_desarrollo_clinicos.sql (modo normal recién cargado
-- o modo reinicio). Solo lee; no modifica datos.
DO $$
DECLARE
    hoy_local date := (now() AT TIME ZONE 'America/El_Salvador')::date;
    c1 constant uuid := 'de110000-0000-4000-8000-000000000001';
    c2 constant uuid := 'de110000-0000-4000-8000-000000000002';
    paciente constant uuid := 'de110001-0000-4000-8000-000000000003';
    doctor constant uuid := 'de110001-0000-4000-8000-000000000002';
BEGIN
    IF (SELECT count(*) FROM public.clinica WHERE id_clinica IN (c1, c2) AND activo) <> 2 THEN
        RAISE EXCEPTION 'Faltan las dos clínicas activas de demostración';
    END IF;
    IF (SELECT count(DISTINCT a.id_clinica) FROM public.persona_rol a JOIN public.rol r ON r.id_rol = a.id_rol
        WHERE a.id_persona = paciente AND r.codigo = 'PACIENTE' AND a.id_clinica IN (c1, c2)) <> 2 THEN
        RAISE EXCEPTION 'paciente.demo debe ser PACIENTE en ambas clínicas';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.persona_rol a JOIN public.rol r ON r.id_rol = a.id_rol
                   WHERE a.id_persona = doctor AND r.codigo = 'ADMINISTRACION' AND a.id_clinica = c1) THEN
        RAISE EXCEPTION 'doctor.demo debe tener ADMINISTRACION en la clínica 1';
    END IF;
    -- El rol inicial de login (asignación de empleado más antigua) sigue siendo DOCTOR en la clínica 1.
    IF (SELECT r.codigo || '@' || a.id_clinica FROM public.persona_rol a JOIN public.rol r ON r.id_rol = a.id_rol
        WHERE a.id_persona = doctor AND r.codigo IN ('ADMINISTRACION', 'DOCTOR')
        ORDER BY a.fecha_creacion, a.id_persona_rol LIMIT 1) <> 'DOCTOR@' || c1 THEN
        RAISE EXCEPTION 'El rol inicial de doctor.demo dejó de ser DOCTOR en la clínica 1';
    END IF;
    IF (SELECT count(*) FROM public.procedimiento_paso p JOIN public.rol r ON r.id_rol = p.id_rol
        WHERE p.id_procedimiento = 'de110006-0000-4000-8000-000000000001' AND r.codigo = 'DOCTOR') <> 3
       OR (SELECT count(*) FROM public.procedimiento_paso_secuencia s
           JOIN public.procedimiento_paso p ON p.id_procedimiento_paso = s.id_procedimiento_paso
           WHERE p.id_procedimiento = 'de110006-0000-4000-8000-000000000001' AND s.tipo_secuencia = 'SIGUIENTE') <> 2 THEN
        RAISE EXCEPTION 'El procedimiento demo requiere 3 pasos DOCTOR y 2 secuencias SIGUIENTE';
    END IF;
    -- Por clínica, una consulta pasada y una futura; hoy, dos en la clínica 1 y una en la 2.
    IF (SELECT count(*) FROM public.consulta c JOIN public.persona_rol a ON a.id_persona_rol = c.id_persona_rol
        WHERE c.id_consulta::text LIKE 'de110009-%' AND a.id_persona = paciente
          AND ((c.fecha_inicio AT TIME ZONE 'America/El_Salvador')::date < hoy_local)) <> 2
       OR (SELECT count(*) FROM public.consulta c JOIN public.persona_rol a ON a.id_persona_rol = c.id_persona_rol
           WHERE c.id_consulta::text LIKE 'de110009-%' AND a.id_persona = paciente
             AND (c.fecha_inicio AT TIME ZONE 'America/El_Salvador')::date = hoy_local) <> 3
       OR (SELECT count(*) FROM public.consulta c
           WHERE c.id_consulta::text LIKE 'de110009-%'
             AND (c.fecha_inicio AT TIME ZONE 'America/El_Salvador')::date > hoy_local) <> 2 THEN
        RAISE EXCEPTION 'Las consultas demo no están en pasado/hoy/futuro (2 pasadas, 3 hoy, 2 futuras)';
    END IF;
    -- Segunda consulta de hoy en la clínica 1: más tarde que la …-2 y con solo el paso 1 PENDIENTE sin valor.
    IF (SELECT a.id_clinica FROM public.consulta c JOIN public.persona_rol a ON a.id_persona_rol = c.id_persona_rol
        WHERE c.id_consulta = 'de110009-0000-4000-8000-000000000007'
          AND (c.fecha_inicio AT TIME ZONE 'America/El_Salvador')::date = hoy_local
          AND c.fecha_fin IS NULL
          AND c.fecha_inicio > (SELECT fecha_inicio FROM public.consulta WHERE id_consulta = 'de110009-0000-4000-8000-000000000002'))
       IS DISTINCT FROM c1
       OR (SELECT string_agg(pp.nombre || '=' || x.estado || CASE WHEN x.valor IS NULL THEN '' ELSE '+valor' END, ',')
           FROM public.consulta_procedimiento_paso x
           JOIN public.consulta_procedimiento cp ON cp.id_consulta_procedimiento = x.id_consulta_procedimiento
           JOIN public.procedimiento_paso pp ON pp.id_procedimiento_paso = x.id_procedimiento_paso
           WHERE cp.id_consulta = 'de110009-0000-4000-8000-000000000007'
             AND cp.id_procedimiento = 'de110006-0000-4000-8000-000000000001')
       IS DISTINCT FROM 'Toma de signos vitales=PENDIENTE' THEN
        RAISE EXCEPTION 'Falta la segunda consulta de hoy de la clínica 1 (…-7: más tarde que …-2, paso 1 PENDIENTE sin valor)';
    END IF;
    -- Consulta de hoy de la clínica 1: paso 1 COMPLETADO con valor, paso 2 PENDIENTE y nada más.
    IF (SELECT string_agg(pp.nombre || '=' || x.estado || CASE WHEN x.valor IS NULL THEN '' ELSE '+valor' END, ',' ORDER BY pp.nombre)
        FROM public.consulta_procedimiento_paso x
        JOIN public.consulta_procedimiento cp ON cp.id_consulta_procedimiento = x.id_consulta_procedimiento
        JOIN public.procedimiento_paso pp ON pp.id_procedimiento_paso = x.id_procedimiento_paso
        WHERE cp.id_consulta = 'de110009-0000-4000-8000-000000000002')
       <> 'Evaluación médica=PENDIENTE,Toma de signos vitales=COMPLETADO+valor' THEN
        RAISE EXCEPTION 'La consulta de hoy no está a medio avanzar (paso 1 COMPLETADO con valor, paso 2 PENDIENTE)';
    END IF;
END $$;
\echo 'Datos clínicos de demostración correctos.'
