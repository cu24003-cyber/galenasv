\set ON_ERROR_STOP on
BEGIN;

DO $$
DECLARE
    persona_a uuid := gen_random_uuid();
    persona_b uuid := gen_random_uuid();
    persona_c uuid := gen_random_uuid();
    cuenta uuid;
    -- Dato sintético de prueba; no es una credencial utilizable y se revierte.
    hash_prueba text := '$argon2id$v=19$m=19456,t=2,p=1$c2FsdC1kZS1wcnVlYmE$aGFzaC1kZS1wcnVlYmE';
    prefijo text := 'prueba-' || gen_random_uuid()::text;
BEGIN
    IF (SELECT count(*) FROM public.rol WHERE codigo IN ('ADMINISTRACION', 'DOCTOR', 'PACIENTE')) <> 3 THEN
        RAISE EXCEPTION 'Faltan los tres códigos de rol';
    END IF;

    INSERT INTO public.persona (id_persona) VALUES (persona_a), (persona_b), (persona_c);
    INSERT INTO public.usuario (id_persona, nombre_usuario, correo, password_hash)
    VALUES (persona_a, prefijo, prefijo || '@example.invalid', hash_prueba)
    RETURNING id_usuario INTO cuenta;

    IF NOT EXISTS (SELECT 1 FROM public.usuario
                   WHERE id_usuario = cuenta AND activo = false AND fecha_creacion IS NOT NULL) THEN
        RAISE EXCEPTION 'La cuenta debe comenzar deshabilitada y con fecha de creación';
    END IF;

    BEGIN
        INSERT INTO public.usuario (id_persona, nombre_usuario, password_hash)
        VALUES (persona_b, upper(prefijo), hash_prueba);
        RAISE EXCEPTION 'Se aceptó un usuario duplicado por mayúsculas';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO public.usuario (id_persona, nombre_usuario, correo, password_hash)
        VALUES (persona_b, prefijo || '-b', upper(prefijo || '@example.invalid'), hash_prueba);
        RAISE EXCEPTION 'Se aceptó un correo duplicado por mayúsculas';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO public.usuario (id_persona, nombre_usuario, password_hash)
        VALUES (persona_a, prefijo || '-otra', hash_prueba);
        RAISE EXCEPTION 'Se aceptó otra cuenta para la misma persona';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO public.usuario (id_persona, nombre_usuario, password_hash)
        VALUES (gen_random_uuid(), prefijo || '-sin-persona', hash_prueba);
        RAISE EXCEPTION 'Se aceptó una persona inexistente';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO public.usuario (id_persona, nombre_usuario, password_hash)
        VALUES (persona_b, prefijo || '-texto', 'contraseña sin hash');
        RAISE EXCEPTION 'Se aceptó una contraseña sin formato Argon2id';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO public.usuario (id_persona, nombre_usuario, password_hash)
        VALUES (persona_b, prefijo || '@example.invalid', hash_prueba);
        RAISE EXCEPTION 'Se aceptó un correo como nombre de usuario';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO public.usuario (id_persona, nombre_usuario, correo, password_hash)
        VALUES (persona_b, prefijo || '-correo', 'correo invalido', hash_prueba);
        RAISE EXCEPTION 'Se aceptó un correo sin formato válido';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    INSERT INTO public.usuario (id_persona, nombre_usuario, password_hash)
    VALUES (persona_b, prefijo || '-b', hash_prueba), (persona_c, prefijo || '-c', hash_prueba);

    BEGIN
        DELETE FROM public.persona WHERE id_persona = persona_a;
        RAISE EXCEPTION 'Se eliminó una persona que tiene una cuenta';
    EXCEPTION WHEN foreign_key_violation OR restrict_violation THEN NULL;
    END;
END $$;

ROLLBACK;
\echo 'Pruebas de cuentas y roles correctas; datos de prueba revertidos.'
