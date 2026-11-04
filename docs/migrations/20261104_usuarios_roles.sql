BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE public.rol ADD COLUMN IF NOT EXISTS codigo varchar(20);
CREATE UNIQUE INDEX IF NOT EXISTS uq_rol_codigo ON public.rol (codigo);

-- Conservar los UUID y nombres que ya usan asignaciones y procedimientos.
DO $$
DECLARE
    base record;
    candidatos uuid[];
BEGIN
    FOR base IN
        SELECT * FROM (VALUES
            ('ADMINISTRACION', 'Administración', ARRAY['administracion', 'administrador', 'administradora', 'administrativo', 'administrativa', 'secretaria', 'secretario']),
            ('DOCTOR', 'Doctor', ARRAY['doctor', 'doctora', 'medico', 'medica']),
            ('PACIENTE', 'Paciente', ARRAY['paciente'])
        ) AS roles(codigo, nombre, alias)
    LOOP
        IF EXISTS (SELECT 1 FROM public.rol WHERE codigo = base.codigo) THEN
            CONTINUE;
        END IF;

        SELECT array_agg(id_rol) INTO candidatos
        FROM public.rol
        WHERE codigo IS NULL
          AND translate(lower(btrim(nombre)), 'áéíóúü', 'aeiouu') = ANY (base.alias);

        IF cardinality(candidatos) > 1 THEN
            RAISE EXCEPTION 'Varios roles corresponden a %. Revise sus asignaciones antes de elegir el rol canónico.', base.codigo;
        ELSIF cardinality(candidatos) = 1 THEN
            UPDATE public.rol SET codigo = base.codigo WHERE id_rol = candidatos[1];
        ELSE
            INSERT INTO public.rol (id_rol, nombre, activo, codigo)
            VALUES (gen_random_uuid(), base.nombre, true, base.codigo);
        END IF;
    END LOOP;
END $$;

CREATE TABLE IF NOT EXISTS public.usuario (
    id_usuario uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    id_persona uuid NOT NULL,
    nombre_usuario varchar(155) NOT NULL,
    correo varchar(254),
    password_hash text NOT NULL,
    activo boolean NOT NULL DEFAULT false,
    fecha_creacion timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_persona UNIQUE (id_persona),
    CONSTRAINT fk_usuario_persona FOREIGN KEY (id_persona)
        REFERENCES public.persona (id_persona) ON UPDATE CASCADE ON DELETE RESTRICT,
    -- El usuario no admite @ para distinguirlo del correo al iniciar sesión.
    CONSTRAINT ck_usuario_nombre CHECK (nombre_usuario ~ '^[A-Za-z0-9][A-Za-z0-9._-]*$'),
    CONSTRAINT ck_usuario_correo CHECK (correo IS NULL OR
        (correo = btrim(correo) AND correo ~ '^[^[:space:]@]+@[^[:space:]@]+$')),
    -- Se exige formato PHC; el backend deberá generar y verificar el hash real.
    CONSTRAINT ck_usuario_password_hash CHECK (password_hash ~
        '^\$argon2id\$v=19\$m=[0-9]+,t=[0-9]+,p=[0-9]+\$[A-Za-z0-9+/]+\$[A-Za-z0-9+/]+$')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usuario_nombre ON public.usuario (lower(nombre_usuario));
CREATE UNIQUE INDEX IF NOT EXISTS uq_usuario_correo ON public.usuario (lower(correo));

COMMENT ON TABLE public.usuario IS 'Una cuenta por persona; sus roles y clínicas provienen de persona_rol.';
COMMENT ON COLUMN public.usuario.password_hash IS 'Hash Argon2id autocontenido en formato PHC; nunca una contraseña en texto plano.';
COMMENT ON COLUMN public.rol.codigo IS 'Código estable de acceso: ADMINISTRACION, DOCTOR o PACIENTE; los demás roles existentes se conservan.';

COMMIT;
