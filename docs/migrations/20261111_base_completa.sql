-- PostgreSQL 13 o posterior. Ejecutar en la base configurada en Liberty.
-- psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261111_base_completa.sql
-- Sustituye la ejecución separada de los dos parches anteriores.
-- Conserva datos y UUID; no crea cuentas ni modifica contraseñas.
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $$ BEGIN
    IF current_setting('server_version_num')::integer < 130000 THEN
        RAISE EXCEPTION 'Se requiere PostgreSQL 13 o posterior.';
    END IF;
END $$;

CREATE SCHEMA IF NOT EXISTS public;

CREATE TABLE IF NOT EXISTS public.clinica (
    id_clinica uuid NOT NULL,
    nombre character varying(255) NOT NULL,
    activo boolean,
    tipo character varying(20),
    comentarios text,
    CONSTRAINT pk_clinica PRIMARY KEY (id_clinica)
);

CREATE TABLE IF NOT EXISTS public.consulta (
    id_consulta uuid NOT NULL,
    fecha_inicio timestamp with time zone DEFAULT now(),
    fecha_fin timestamp with time zone,
    referencia_externa text,
    observaciones text,
    id_persona_rol uuid,
    CONSTRAINT pk_consulta PRIMARY KEY (id_consulta)
);

CREATE TABLE IF NOT EXISTS public.consulta_procedimiento (
    id_consulta_procedimiento uuid NOT NULL,
    id_consulta uuid,
    id_procedimiento uuid,
    fecha_inicio timestamp with time zone,
    fecha_fin timestamp with time zone,
    observaciones text,
    CONSTRAINT pk_consulta_procedimiento PRIMARY KEY (id_consulta_procedimiento)
);

CREATE TABLE IF NOT EXISTS public.consulta_procedimiento_paso (
    id_consulta_procedimiento_paso uuid NOT NULL,
    id_consulta_procedimiento uuid,
    id_persona_rol uuid,
    fecha_inicio timestamp with time zone DEFAULT now(),
    fecha_fin timestamp with time zone,
    estado character varying(20),
    id_procedimiento_paso uuid,
    valor text,
    CONSTRAINT pk_consulta_procedimiento_paso PRIMARY KEY (id_consulta_procedimiento_paso)
);

CREATE TABLE IF NOT EXISTS public.documento (
    id_documento uuid NOT NULL,
    id_persona uuid,
    id_tipo_documento uuid,
    valor text,
    ruta_fisica text,
    CONSTRAINT pk_documento PRIMARY KEY (id_documento)
);

CREATE TABLE IF NOT EXISTS public.examen (
    id_examen uuid NOT NULL,
    nombre character varying(255),
    activo boolean DEFAULT true,
    observaciones text,
    CONSTRAINT pk_examen PRIMARY KEY (id_examen)
);

CREATE TABLE IF NOT EXISTS public.examen_resultado (
    id_examen_resultado uuid NOT NULL,
    id_orden_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    resultado text,
    interpretacion text,
    ruta_atestado text,
    CONSTRAINT pk_examen_resultado PRIMARY KEY (id_examen_resultado)
);

CREATE TABLE IF NOT EXISTS public.examen_tipo_examen (
    id_examen_tipo_examen uuid NOT NULL,
    id_examen uuid,
    id_tipo_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    observaciones text,
    CONSTRAINT pk_examen_tipo_examen PRIMARY KEY (id_examen_tipo_examen)
);

CREATE TABLE IF NOT EXISTS public.medio_contacto (
    id_medio_contacto uuid NOT NULL,
    id_persona uuid,
    id_tipo_medio_contacto uuid,
    valor text,
    fecha_creacion timestamp with time zone DEFAULT now(),
    CONSTRAINT pk_medio_contacto PRIMARY KEY (id_medio_contacto)
);

CREATE TABLE IF NOT EXISTS public.orden_examen (
    id_orden_examen uuid NOT NULL,
    id_consulta_procedimiento_paso uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    indicaciones text,
    CONSTRAINT pk_orden_examen PRIMARY KEY (id_orden_examen)
);

CREATE TABLE IF NOT EXISTS public.persona (
    id_persona uuid NOT NULL,
    nombres character varying(255),
    apellidos character varying(255),
    fecha_nacimiento timestamp with time zone,
    fecha_creacion timestamp with time zone DEFAULT now(),
    CONSTRAINT pk_persona PRIMARY KEY (id_persona)
);

CREATE TABLE IF NOT EXISTS public.persona_rol (
    id_persona_rol uuid NOT NULL,
    id_persona uuid,
    id_rol uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    id_clinica uuid,
    CONSTRAINT pk_persona_rol PRIMARY KEY (id_persona_rol)
);

CREATE TABLE IF NOT EXISTS public.procedimiento (
    id_procedimiento uuid NOT NULL,
    nombre character varying(155),
    activo boolean,
    observaciones text,
    CONSTRAINT pk_procedimiento PRIMARY KEY (id_procedimiento)
);

CREATE TABLE IF NOT EXISTS public.procedimiento_paso (
    id_procedimiento_paso uuid NOT NULL,
    id_procedimiento uuid,
    nombre character varying(155),
    indica_fin boolean DEFAULT false,
    id_rol uuid,
    CONSTRAINT pk_procedimiento_paso PRIMARY KEY (id_procedimiento_paso)
);

CREATE TABLE IF NOT EXISTS public.procedimiento_paso_examen (
    id_procedimiento_paso_examen uuid NOT NULL,
    id_procedimiento_paso uuid,
    id_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    activo boolean DEFAULT true,
    observaciones text,
    CONSTRAINT pk_procedimiento_paso_examen PRIMARY KEY (id_procedimiento_paso_examen)
);

CREATE TABLE IF NOT EXISTS public.procedimiento_paso_secuencia (
    id_procedimiento_paso_secuencia uuid NOT NULL,
    id_procedimiento_paso uuid,
    id_procedimiento_paso_referencia uuid,
    tipo_secuencia character varying(20),
    CONSTRAINT pk_procedimiento_paso_secuencia PRIMARY KEY (id_procedimiento_paso_secuencia)
);

CREATE TABLE IF NOT EXISTS public.rol (
    id_rol uuid NOT NULL,
    nombre character varying(155),
    activo boolean,
    observaciones text,
    codigo character varying(20),
    CONSTRAINT pk_rol PRIMARY KEY (id_rol)
);

CREATE TABLE IF NOT EXISTS public.tipo_documento (
    id_tipo_documento uuid NOT NULL,
    nombre character varying(155),
    indicaciones text,
    expresion_regular text DEFAULT '.'::text,
    activo boolean,
    CONSTRAINT pk_tipo_documento PRIMARY KEY (id_tipo_documento)
);

CREATE TABLE IF NOT EXISTS public.tipo_examen (
    id_tipo_examen uuid NOT NULL,
    nombre character varying,
    activo boolean,
    observaciones text,
    CONSTRAINT pk_tipo_examen PRIMARY KEY (id_tipo_examen)
);

CREATE TABLE IF NOT EXISTS public.tipo_medio_contacto (
    id_tipo_medio_contacto uuid NOT NULL,
    nombre character varying(155),
    indicaciones text,
    expresion_regular text,
    activo boolean,
    CONSTRAINT pk_tipo_medio_contacto PRIMARY KEY (id_tipo_medio_contacto)
);

-- Columnas que no estaban en el esquema clínico original.
ALTER TABLE public.consulta_procedimiento_paso
    ADD COLUMN IF NOT EXISTS id_procedimiento_paso uuid,
    ADD COLUMN IF NOT EXISTS valor text;


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


-- Las claves se añaden una sola vez y se comprueban también en bases anteriores.
-- Si existen referencias huérfanas, toda la migración se revierte.
DO $$
DECLARE clave record;
BEGIN
    FOR clave IN SELECT * FROM (VALUES
        ('consulta', 'consulta_persona_rol', 'FOREIGN KEY (id_persona_rol) REFERENCES public.persona_rol(id_persona_rol) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('consulta_procedimiento', 'fk_consulta_procedimiento_consulta', 'FOREIGN KEY (id_consulta) REFERENCES public.consulta(id_consulta) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('consulta_procedimiento', 'fk_consulta_procedimiento_procedimiento', 'FOREIGN KEY (id_procedimiento) REFERENCES public.procedimiento(id_procedimiento) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('consulta_procedimiento_paso', 'fk_consulta_procedimiento_paso_consulta_paso', 'FOREIGN KEY (id_procedimiento_paso) REFERENCES public.procedimiento_paso(id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('consulta_procedimiento_paso', 'fk_consulta_procedimiento_paso_consulta_procedimiento', 'FOREIGN KEY (id_consulta_procedimiento) REFERENCES public.consulta_procedimiento(id_consulta_procedimiento) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('consulta_procedimiento_paso', 'fk_consulta_procedimiento_paso_persona_rol', 'FOREIGN KEY (id_persona_rol) REFERENCES public.persona_rol(id_persona_rol) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('documento', 'fk_documento_persona', 'FOREIGN KEY (id_persona) REFERENCES public.persona(id_persona) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('documento', 'fk_documento_tipo_documento', 'FOREIGN KEY (id_tipo_documento) REFERENCES public.tipo_documento(id_tipo_documento) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('examen_resultado', 'fk_examen_resultado_orden_examen', 'FOREIGN KEY (id_orden_examen) REFERENCES public.orden_examen(id_orden_examen) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('examen_tipo_examen', 'fk_examen_tipo_examen_examen', 'FOREIGN KEY (id_examen) REFERENCES public.examen(id_examen) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('examen_tipo_examen', 'fk_examen_tipo_examen_tipo_examen', 'FOREIGN KEY (id_tipo_examen) REFERENCES public.tipo_examen(id_tipo_examen) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('medio_contacto', 'fk_medio_contacto_persona', 'FOREIGN KEY (id_persona) REFERENCES public.persona(id_persona) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('medio_contacto', 'fk_medio_contacto_tipo_medio_contacto', 'FOREIGN KEY (id_tipo_medio_contacto) REFERENCES public.tipo_medio_contacto(id_tipo_medio_contacto) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('orden_examen', 'fk_orden_examen_consulta_procedimiento_paso', 'FOREIGN KEY (id_consulta_procedimiento_paso) REFERENCES public.consulta_procedimiento_paso(id_consulta_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('persona_rol', 'fk_persona_rol_clinica', 'FOREIGN KEY (id_clinica) REFERENCES public.clinica(id_clinica)'),
        ('persona_rol', 'fk_persona_rol_persona', 'FOREIGN KEY (id_persona) REFERENCES public.persona(id_persona) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('persona_rol', 'fk_persona_rol_rol', 'FOREIGN KEY (id_rol) REFERENCES public.rol(id_rol) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('procedimiento_paso_examen', 'fk_procedimiento_paso_examen_examen', 'FOREIGN KEY (id_examen) REFERENCES public.examen(id_examen) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('procedimiento_paso_examen', 'fk_procedimiento_paso_examen_procedimiento_examen', 'FOREIGN KEY (id_procedimiento_paso) REFERENCES public.procedimiento_paso(id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('procedimiento_paso', 'fk_procedimiento_paso_procedimiento', 'FOREIGN KEY (id_procedimiento) REFERENCES public.procedimiento(id_procedimiento) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('procedimiento_paso', 'fk_procedimiento_paso_rol', 'FOREIGN KEY (id_rol) REFERENCES public.rol(id_rol) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('procedimiento_paso_secuencia', 'fk_procedimiento_paso_secuencia_procedimiento_paso', 'FOREIGN KEY (id_procedimiento_paso) REFERENCES public.procedimiento_paso(id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('usuario', 'fk_usuario_persona', 'FOREIGN KEY (id_persona) REFERENCES public.persona(id_persona) ON UPDATE CASCADE ON DELETE RESTRICT'),
        ('procedimiento_paso_secuencia', 'fk_procedimiento_paso_secuencia_referencia', 'FOREIGN KEY (id_procedimiento_paso_referencia) REFERENCES public.procedimiento_paso(id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT')
    ) AS claves(tabla, nombre, definicion)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conrelid = ('public.' || clave.tabla)::regclass AND conname = clave.nombre) THEN
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I %s NOT VALID', clave.tabla, clave.nombre, clave.definicion);
        END IF;
        EXECUTE format('ALTER TABLE public.%I VALIDATE CONSTRAINT %I', clave.tabla, clave.nombre);
    END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS fki_fk_persona_rol_clinica ON public.persona_rol (id_clinica);
COMMIT;
