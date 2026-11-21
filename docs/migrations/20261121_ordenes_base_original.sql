-- Solo para una copia/base propia que ya tiene orden_examen.id_examen.
-- Detener la aplicación y hacer pg_dump completo antes de ejecutar con ON_ERROR_STOP=1.
-- No agrega columnas ni elimina datos; conserva id_examen antiguo como referencia histórica.
-- El backend nuevo usa exclusivamente el contenido estructurado; no volver al WAR anterior.
BEGIN;
LOCK TABLE public.orden_examen IN SHARE ROW EXCLUSIVE MODE;
DO $conversion$
DECLARE
    orden record;
    contenido json;
    cabecera text;
    identificador text;
    texto_json text;
    convertidas integer := 0;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='orden_examen' AND column_name='id_examen') THEN
        RAISE NOTICE 'Base original: no hay columna adicional ni conversión que ejecutar.';
        RETURN;
    END IF;
    FOR orden IN EXECUTE 'SELECT id_orden_examen, id_examen, indicaciones FROM public.orden_examen WHERE id_examen IS NOT NULL' LOOP
        IF starts_with(orden.indicaciones, '@galenosv/orden/') THEN
            -- Un formato desconocido o dañado exige revisión; no sobrescribirlo.
            IF NOT starts_with(orden.indicaciones, '@galenosv/orden/v1:') THEN
                RAISE EXCEPTION 'Formato desconocido en orden %', orden.id_orden_examen;
            END IF;
            identificador := split_part(substring(orden.indicaciones FROM length('@galenosv/orden/v1:')+1), ':', 1);
            IF identificador <> '-' AND identificador::uuid::text <> identificador THEN
                RAISE EXCEPTION 'UUID no canónico en orden %', orden.id_orden_examen;
            END IF;
            cabecera := '@galenosv/orden/v1:' || identificador || ':';
            texto_json := substring(orden.indicaciones FROM length(cabecera)+1);
            contenido := texto_json::json;
            IF json_typeof(contenido) <> 'object' THEN
                RAISE EXCEPTION 'Contenido inválido en orden %', orden.id_orden_examen;
            END IF;
            IF (SELECT count(*) FROM json_object_keys(contenido)) <> 1
               OR NOT (contenido::jsonb ? 'indicaciones')
               OR json_typeof(contenido->'indicaciones') NOT IN ('string', 'null') THEN
                RAISE EXCEPTION 'Indicaciones inválidas en orden %', orden.id_orden_examen;
            END IF;
            CONTINUE;
        END IF;
        UPDATE public.orden_examen
        SET indicaciones = '@galenosv/orden/v1:' || orden.id_examen::text || ':'
                           || json_build_object('indicaciones', orden.indicaciones)::text
        WHERE id_orden_examen = orden.id_orden_examen;
        convertidas := convertidas + 1;
    END LOOP;
    RAISE NOTICE 'Órdenes convertidas: %', convertidas;
END
$conversion$;
COMMIT;
