\set ON_ERROR_STOP on
-- Ejecutar después de 20261113_datos_demo_rubrica.sql y 20261113_datos_demo_rubrica_consultas.sql
-- (recién cargados o con -v reiniciar=1). Solo lee; no modifica datos.
-- Comprueba docs/datos-demo-rubrica.md: catálogos, personas, cuentas y las 11 consultas.
DO $$
DECLARE
    hoy date := (now() AT TIME ZONE 'America/El_Salvador')::date;
    obtenido text;
    esperado text;
BEGIN
    -- 1. Clínicas
    SELECT string_agg(nombre || '/' || CASE WHEN activo THEN 'activa' ELSE 'inactiva' END || '/' || tipo, ', ' ORDER BY nombre)
      INTO obtenido FROM public.clinica WHERE id_clinica::text LIKE 'de120000-%';
    esperado := 'Clínica San Salvador/activa/Especialidades, Clínica Santa Ana/activa/General, Clínica Sonsonate/inactiva/Pediátrica';
    IF obtenido IS DISTINCT FROM esperado THEN RAISE EXCEPTION 'Clínicas: % (esperado %)', obtenido, esperado; END IF;

    -- 2. Roles: cuatro activos y uno inactivo; Atención al público conserva el código ADMINISTRACION.
    SELECT string_agg(codigo || '=' || nombre || CASE WHEN activo THEN '' ELSE '(inactivo)' END, ', ' ORDER BY codigo)
      INTO obtenido FROM public.rol WHERE codigo IN ('ADMINISTRACION', 'DOCTOR', 'ENFERMERA', 'FARMACEUTICO', 'PACIENTE');
    esperado := 'ADMINISTRACION=Atención al público, DOCTOR=Doctor, ENFERMERA=Enfermera, FARMACEUTICO=Farmacéutico(inactivo), PACIENTE=Paciente';
    IF obtenido IS DISTINCT FROM esperado THEN RAISE EXCEPTION 'Roles: % (esperado %)', obtenido, esperado; END IF;

    -- 3 y 4. Tipos de medio de contacto y de documento: dos activos con expresión regular y uno inactivo.
    IF (SELECT count(*) FROM public.tipo_medio_contacto WHERE nombre IN ('Teléfono', 'Correo electrónico') AND activo
            AND expresion_regular IS NOT NULL AND indicaciones LIKE '%Ejemplo:%') < 2
       OR NOT EXISTS (SELECT 1 FROM public.tipo_medio_contacto WHERE nombre = 'Fax' AND NOT activo) THEN
        RAISE EXCEPTION 'Tipos de medio de contacto: se esperan Teléfono y Correo electrónico activos y Fax inactivo';
    END IF;
    IF (SELECT count(*) FROM public.tipo_documento WHERE nombre IN ('DUI', 'Pasaporte') AND activo
            AND expresion_regular IS NOT NULL AND indicaciones LIKE '%Ejemplo:%') < 2
       OR NOT EXISTS (SELECT 1 FROM public.tipo_documento WHERE nombre = 'Licencia de conducir' AND NOT activo) THEN
        RAISE EXCEPTION 'Tipos de documento: se esperan DUI y Pasaporte activos y Licencia de conducir inactiva';
    END IF;
    IF '01000001-1' !~ (SELECT expresion_regular FROM public.tipo_documento WHERE nombre = 'DUI' AND activo LIMIT 1)
       OR 'A10000001' !~ (SELECT expresion_regular FROM public.tipo_documento WHERE nombre = 'Pasaporte' AND activo LIMIT 1)
       OR '7100-0001' !~ (SELECT expresion_regular FROM public.tipo_medio_contacto WHERE nombre = 'Teléfono' AND activo LIMIT 1)
       OR 'ana.martinez@galenosv.test' !~ (SELECT expresion_regular FROM public.tipo_medio_contacto
                                           WHERE nombre = 'Correo electrónico' AND activo LIMIT 1) THEN
        RAISE EXCEPTION 'Las expresiones regulares no aceptan los valores demo';
    END IF;

    -- 5 y 6. Tipos de examen y exámenes con su tipo.
    SELECT string_agg(e.nombre || CASE WHEN e.activo THEN '' ELSE '(inactivo)' END || '/' || t.nombre
                      || CASE WHEN t.activo THEN '' ELSE '(inactivo)' END, ', ' ORDER BY e.nombre)
      INTO obtenido FROM public.examen e
      JOIN public.examen_tipo_examen et ON et.id_examen = e.id_examen
      JOIN public.tipo_examen t ON t.id_tipo_examen = et.id_tipo_examen
     WHERE e.id_examen::text LIKE 'de120005-%';
    esperado := 'Electrocardiograma(inactivo)/Imagenología, Examen general de orina/Laboratorio clínico, '
             || 'Glucosa en ayunas/Laboratorio clínico, Hemograma completo/Laboratorio clínico, Rayos X de tórax/Imagenología';
    IF obtenido IS DISTINCT FROM esperado THEN RAISE EXCEPTION 'Exámenes: % (esperado %)', obtenido, esperado; END IF;
    IF NOT EXISTS (SELECT 1 FROM public.tipo_examen WHERE id_tipo_examen = 'de120004-0000-4000-8000-000000000003'
                   AND nombre = 'Patología' AND NOT activo) THEN
        RAISE EXCEPTION 'Falta el tipo de examen inactivo Patología';
    END IF;

    -- 7. Procedimientos: pasos en orden por las secuencias, rol de cada paso y finalización.
    SELECT string_agg(x.ruta, ' || ' ORDER BY x.ruta) INTO obtenido FROM (
        WITH RECURSIVE cadena AS (
            SELECT p.id_procedimiento, p.id_procedimiento_paso, 1 AS orden,
                   p.nombre || '[' || r.codigo || CASE WHEN p.indica_fin THEN ',fin' ELSE '' END || ']' AS texto
              FROM public.procedimiento_paso p JOIN public.rol r ON r.id_rol = p.id_rol
             WHERE p.id_procedimiento::text LIKE 'de120007-%'
               AND NOT EXISTS (SELECT 1 FROM public.procedimiento_paso_secuencia s
                               WHERE s.id_procedimiento_paso_referencia = p.id_procedimiento_paso AND s.tipo_secuencia = 'SIGUIENTE')
            UNION ALL
            SELECT c.id_procedimiento, p.id_procedimiento_paso, c.orden + 1,
                   c.texto || ' > ' || p.nombre || '[' || r.codigo || CASE WHEN p.indica_fin THEN ',fin' ELSE '' END || ']'
              FROM cadena c
              JOIN public.procedimiento_paso_secuencia s ON s.id_procedimiento_paso = c.id_procedimiento_paso AND s.tipo_secuencia = 'SIGUIENTE'
              JOIN public.procedimiento_paso p ON p.id_procedimiento_paso = s.id_procedimiento_paso_referencia
              JOIN public.rol r ON r.id_rol = p.id_rol
             WHERE c.orden < 10)
        SELECT pr.nombre || ': ' || c.texto AS ruta FROM cadena c JOIN public.procedimiento pr USING (id_procedimiento)
         WHERE c.orden = 3 AND pr.activo) x;
    esperado := 'Consulta general: Triaje[ENFERMERA] > Consulta médica[DOCTOR] > Entrega de indicaciones[ADMINISTRACION,fin] || '
             || 'Control de hipertensión: Toma de presión arterial[ENFERMERA] > Evaluación médica[DOCTOR] > Programación de seguimiento[ADMINISTRACION,fin]';
    IF obtenido IS DISTINCT FROM esperado THEN RAISE EXCEPTION 'Procedimientos: % (esperado %)', obtenido, esperado; END IF;

    -- Exámenes asignables (asociación y examen activos, con tipo activo).
    SELECT string_agg(e.nombre, ', ' ORDER BY e.nombre) INTO obtenido
      FROM public.procedimiento_paso_examen pe JOIN public.examen e ON e.id_examen = pe.id_examen
     WHERE pe.id_procedimiento_paso = 'de120008-0000-4000-8000-000000000002' AND pe.activo AND e.activo
       AND EXISTS (SELECT 1 FROM public.examen_tipo_examen et JOIN public.tipo_examen t ON t.id_tipo_examen = et.id_tipo_examen
                   WHERE et.id_examen = e.id_examen AND t.activo);
    IF obtenido IS DISTINCT FROM 'Examen general de orina, Glucosa en ayunas, Hemograma completo, Rayos X de tórax' THEN
        RAISE EXCEPTION 'Asignables de Consulta médica: %', obtenido;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.procedimiento_paso_examen pe JOIN public.examen e ON e.id_examen = pe.id_examen
                   WHERE pe.id_procedimiento_paso = 'de120008-0000-4000-8000-000000000002' AND pe.activo
                     AND e.nombre = 'Electrocardiograma' AND NOT e.activo) THEN
        RAISE EXCEPTION 'Falta Electrocardiograma (inactivo) asociado a Consulta médica';
    END IF;
    SELECT string_agg(e.nombre, ', ' ORDER BY e.nombre) INTO obtenido
      FROM public.procedimiento_paso_examen pe JOIN public.examen e ON e.id_examen = pe.id_examen
     WHERE pe.id_procedimiento_paso = 'de120008-0000-4000-8000-000000000005' AND pe.activo AND e.activo;
    IF obtenido IS DISTINCT FROM 'Glucosa en ayunas, Rayos X de tórax' THEN
        RAISE EXCEPTION 'Asignables de Evaluación médica: %', obtenido;
    END IF;

    -- 8. Personas, documentos, contactos, roles y cuentas.
    SELECT string_agg(p.nombres || ' ' || p.apellidos || ' ' || to_char((p.fecha_nacimiento AT TIME ZONE 'America/El_Salvador')::date, 'YYYY-MM-DD')
                      || ' [' || coalesce((SELECT string_agg(t.nombre || ' ' || d.valor, ', ' ORDER BY t.nombre)
                                             FROM public.documento d JOIN public.tipo_documento t USING (id_tipo_documento)
                                            WHERE d.id_persona = p.id_persona), '') || ']'
                      || ' {' || coalesce((SELECT string_agg(r.codigo || '@' || c.nombre, ', ' ORDER BY r.codigo, c.nombre)
                                             FROM public.persona_rol a JOIN public.rol r USING (id_rol) JOIN public.clinica c USING (id_clinica)
                                            WHERE a.id_persona = p.id_persona), '') || '}'
                      || coalesce(' @' || (SELECT u.nombre_usuario FROM public.usuario u WHERE u.id_persona = p.id_persona AND u.activo), ''),
                      E'\n' ORDER BY p.id_persona)
      INTO obtenido FROM public.persona p WHERE p.id_persona::text LIKE 'de12000b-%';
    esperado := concat_ws(E'\n',
        'Carlos Gómez 1980-03-15 [DUI 01000001-1, Pasaporte A10000001] {DOCTOR@Clínica San Salvador, DOCTOR@Clínica Santa Ana} @carlos.gomez',
        'Lucía Pérez 1988-07-22 [DUI 01000002-2] {ENFERMERA@Clínica Santa Ana} @lucia.perez',
        'Marta López 1990-11-05 [DUI 01000003-3] {ADMINISTRACION@Clínica San Salvador, ADMINISTRACION@Clínica Santa Ana} @marta.lopez',
        'Ana Martínez 1992-04-10 [DUI 01000004-4, Pasaporte B10000004] {PACIENTE@Clínica San Salvador, PACIENTE@Clínica Santa Ana}',
        'Pedro Hernández 1975-01-30 [DUI 01000005-5] {}',
        'Rosa Méndez 1985-09-18 [DUI 01000006-6] {ENFERMERA@Clínica San Salvador} @rosa.mendez',
        'Jorge Ruiz 1978-12-02 [DUI 01000007-7] {DOCTOR@Clínica San Salvador} @jorge.ruiz',
        'Julia Flores 1960-06-25 [DUI 01000008-8] {PACIENTE@Clínica Santa Ana}',
        'Juan Vásquez 2001-02-14 [DUI 01000009-9] {PACIENTE@Clínica Santa Ana}');
    IF obtenido IS DISTINCT FROM esperado THEN RAISE EXCEPTION E'Personas:\n%\nEsperado:\n%', obtenido, esperado; END IF;
    IF (SELECT count(*) FROM public.medio_contacto m JOIN public.tipo_medio_contacto t USING (id_tipo_medio_contacto)
        WHERE m.id_medio_contacto::text LIKE 'de12000d-%' AND t.activo AND m.valor ~ t.expresion_regular) <> 18 THEN
        RAISE EXCEPTION 'Cada persona demo debe tener un teléfono y un correo válidos';
    END IF;
    -- Rol inicial de login de Carlos (asignación de empleado más antigua): Doctor en Santa Ana.
    IF (SELECT c.nombre FROM public.persona_rol a JOIN public.rol r USING (id_rol) JOIN public.clinica c USING (id_clinica)
        WHERE a.id_persona = 'de12000b-0000-4000-8000-000000000001' AND r.codigo <> 'PACIENTE'
        ORDER BY a.fecha_creacion, a.id_persona_rol LIMIT 1) IS DISTINCT FROM 'Clínica Santa Ana' THEN
        RAISE EXCEPTION 'El rol inicial de Carlos debe ser Doctor en Clínica Santa Ana';
    END IF;

    -- 9. Consultas: día relativo, hora local, procedimiento, estado de cada paso y responsable.
    SELECT string_agg(x.linea, E'\n' ORDER BY x.n) INTO obtenido FROM (
        SELECT right(c.id_consulta::text, 2)::int AS n,
               pac.nombres || ' ' || cl.nombre || ' d' || ((c.fecha_inicio AT TIME ZONE 'America/El_Salvador')::date - hoy)
               || ' ' || to_char(c.fecha_inicio AT TIME ZONE 'America/El_Salvador', 'HH24:MI') || ' ' || pr.nombre
               || CASE WHEN c.fecha_fin IS NULL THEN ' abierta' ELSE ' cerrada' END || ': '
               || (SELECT string_agg(pp.nombre || '=' || x.estado || CASE WHEN x.valor IS NULL THEN '' ELSE '+valor' END
                                     || '(' || rp.nombres || ')', ', ' ORDER BY x.fecha_inicio)
                     FROM public.consulta_procedimiento_paso x
                     JOIN public.procedimiento_paso pp ON pp.id_procedimiento_paso = x.id_procedimiento_paso
                     JOIN public.persona_rol ra ON ra.id_persona_rol = x.id_persona_rol
                     JOIN public.persona rp ON rp.id_persona = ra.id_persona
                    WHERE x.id_consulta_procedimiento = cp.id_consulta_procedimiento) AS linea
          FROM public.consulta c
          JOIN public.persona_rol a ON a.id_persona_rol = c.id_persona_rol
          JOIN public.persona pac ON pac.id_persona = a.id_persona
          JOIN public.clinica cl ON cl.id_clinica = a.id_clinica
          JOIN public.consulta_procedimiento cp ON cp.id_consulta = c.id_consulta
          JOIN public.procedimiento pr ON pr.id_procedimiento = cp.id_procedimiento
         WHERE c.id_consulta::text LIKE 'de120010-%') x;
    esperado := concat_ws(E'\n',
        'Ana Clínica Santa Ana d-20 09:00 Consulta general cerrada: Triaje=COMPLETADO+valor(Lucía), Consulta médica=COMPLETADO+valor(Carlos), Entrega de indicaciones=COMPLETADO+valor(Marta)',
        'Julia Clínica Santa Ana d-1 15:00 Control de hipertensión cerrada: Toma de presión arterial=COMPLETADO+valor(Lucía), Evaluación médica=COMPLETADO+valor(Carlos), Programación de seguimiento=COMPLETADO+valor(Marta)',
        'Ana Clínica Santa Ana d0 08:00 Consulta general abierta: Triaje=COMPLETADO+valor(Lucía), Consulta médica=PENDIENTE(Carlos)',
        'Julia Clínica Santa Ana d0 09:15 Control de hipertensión abierta: Toma de presión arterial=PENDIENTE(Lucía)',
        'Juan Clínica Santa Ana d0 10:30 Consulta general abierta: Triaje=PENDIENTE(Lucía)',
        'Ana Clínica Santa Ana d0 11:45 Consulta general abierta: Triaje=COMPLETADO+valor(Lucía), Consulta médica=COMPLETADO+valor(Carlos), Entrega de indicaciones=PENDIENTE(Marta)',
        'Juan Clínica Santa Ana d3 08:30 Consulta general abierta: Triaje=PENDIENTE(Lucía)',
        'Ana Clínica Santa Ana d10 10:00 Control de hipertensión abierta: Toma de presión arterial=PENDIENTE(Lucía)',
        'Ana Clínica San Salvador d-10 14:00 Consulta general cerrada: Triaje=COMPLETADO+valor(Rosa), Consulta médica=COMPLETADO+valor(Jorge), Entrega de indicaciones=COMPLETADO+valor(Marta)',
        'Ana Clínica San Salvador d0 13:00 Consulta general abierta: Triaje=COMPLETADO+valor(Rosa), Consulta médica=PENDIENTE(Carlos)',
        'Ana Clínica San Salvador d5 09:00 Control de hipertensión abierta: Toma de presión arterial=PENDIENTE(Rosa)');
    IF obtenido IS DISTINCT FROM esperado THEN RAISE EXCEPTION E'Consultas:\n%\nEsperado:\n%', obtenido, esperado; END IF;

    -- Cada paso está asignado a alguien con el rol del paso en la clínica de la consulta.
    IF EXISTS (SELECT 1 FROM public.consulta_procedimiento_paso x
               JOIN public.consulta_procedimiento cp USING (id_consulta_procedimiento)
               JOIN public.consulta c ON c.id_consulta = cp.id_consulta
               JOIN public.persona_rol pac ON pac.id_persona_rol = c.id_persona_rol
               JOIN public.procedimiento_paso pp ON pp.id_procedimiento_paso = x.id_procedimiento_paso
               JOIN public.persona_rol ra ON ra.id_persona_rol = x.id_persona_rol
               WHERE c.id_consulta::text LIKE 'de120010-%'
                 AND (ra.id_rol <> pp.id_rol OR ra.id_clinica <> pac.id_clinica)) THEN
        RAISE EXCEPTION 'Hay pasos demo asignados a alguien sin el rol del paso en la clínica de la consulta';
    END IF;

    -- Órdenes: Hemograma completo en la Consulta médica de SA-1 y Glucosa en ayunas en la de SA-6.
    SELECT string_agg(right(cp.id_consulta::text, 2)::int || ':' || pp.nombre || ':' || e.nombre, ', ' ORDER BY cp.id_consulta)
      INTO obtenido FROM public.orden_examen o
      JOIN public.examen e ON e.id_examen = o.id_examen
      JOIN public.consulta_procedimiento_paso x ON x.id_consulta_procedimiento_paso = o.id_consulta_procedimiento_paso
      JOIN public.procedimiento_paso pp ON pp.id_procedimiento_paso = x.id_procedimiento_paso
      JOIN public.consulta_procedimiento cp ON cp.id_consulta_procedimiento = x.id_consulta_procedimiento
     WHERE cp.id_consulta::text LIKE 'de120010-%';
    IF obtenido IS DISTINCT FROM '1:Consulta médica:Hemograma completo, 6:Consulta médica:Glucosa en ayunas' THEN
        RAISE EXCEPTION 'Órdenes demo: %', obtenido;
    END IF;
END $$;
\echo 'Datos demo del guion correctos.'
