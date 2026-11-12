\set ON_ERROR_STOP on
-- Opcional: solo para el entorno de desarrollo de cada compañero.
-- password_hash debe proceder de ContrasenaService, nunca de una contraseña sin hash.
-- Desde la HU2 el login es por documento y la columna no se usa; el script genera un hash aleatorio.
\if :{?password_hash}
\else
DO $$ BEGIN RAISE EXCEPTION 'Falta password_hash. Utilice scripts/preparar-bd-desarrollo.sh o genere el hash con ContrasenaService.'; END $$;
\endif
BEGIN;
SET LOCAL lock_timeout = '5s';
CREATE TEMP TABLE clave_desarrollo (
    hash text NOT NULL CHECK (hash ~ '^\$argon2id\$v=19\$m=19456,t=2,p=1\$[A-Za-z0-9+/]{22}\$[A-Za-z0-9+/]{43}$')
) ON COMMIT DROP;
INSERT INTO clave_desarrollo VALUES (:'password_hash');

DO $$ BEGIN
    IF (SELECT count(*) FROM public.rol WHERE codigo IN ('ADMINISTRACION','DOCTOR','PACIENTE') AND activo=true) <> 3 THEN
        RAISE EXCEPTION 'Se requieren los tres roles activos. Aplique primero la migración completa y revise los roles inactivos existentes.';
    END IF;
END $$;

INSERT INTO public.clinica (id_clinica,nombre,activo,tipo,comentarios)
VALUES ('de110000-0000-4000-8000-000000000001','Clínica de demostración',true,'Clínica','Entorno local de desarrollo')
ON CONFLICT (id_clinica) DO NOTHING;

CREATE TEMP TABLE cuentas_desarrollo (
    numero integer, nombre_usuario text, nombres text, codigo text
) ON COMMIT DROP;
INSERT INTO cuentas_desarrollo VALUES
    (1,'admin.demo','Administración','ADMINISTRACION'),
    (2,'doctor.demo','Doctor','DOCTOR'),
    (3,'paciente.demo','Paciente','PACIENTE');

INSERT INTO public.persona (id_persona,nombres,apellidos,fecha_nacimiento)
SELECT ('de110001-0000-4000-8000-' || lpad(numero::text,12,'0'))::uuid,nombres,'Demostración','2000-01-01T00:00:00Z'
FROM cuentas_desarrollo
ON CONFLICT (id_persona) DO NOTHING;

INSERT INTO public.persona_rol (id_persona_rol,id_persona,id_rol,id_clinica)
SELECT ('de110002-0000-4000-8000-' || lpad(numero::text,12,'0'))::uuid,
       ('de110001-0000-4000-8000-' || lpad(numero::text,12,'0'))::uuid,
       r.id_rol,'de110000-0000-4000-8000-000000000001'
FROM cuentas_desarrollo c JOIN public.rol r ON r.codigo=c.codigo
ON CONFLICT (id_persona_rol) DO NOTHING;

-- HU2: tipo de documento DUI. Si ya existe uno con ese nombre se reutiliza.
INSERT INTO public.tipo_documento (id_tipo_documento,nombre,indicaciones,expresion_regular,activo)
SELECT 'de110004-0000-4000-8000-000000000001','DUI',
       'Ocho dígitos, guion y dígito verificador. Ejemplo: 00000000-0','^[0-9]{8}-[0-9]$',true
WHERE NOT EXISTS (SELECT 1 FROM public.tipo_documento WHERE lower(btrim(nombre))='dui')
ON CONFLICT (id_tipo_documento) DO NOTHING;

-- Números ficticios (prefijo 00000000); no corresponden a ninguna persona real.
CREATE TEMP TABLE documentos_desarrollo (numero integer, valor text) ON COMMIT DROP;
INSERT INTO documentos_desarrollo VALUES (1,'00000000-1'),(2,'00000000-2'),(3,'00000000-3');

INSERT INTO public.documento (id_documento,id_persona,id_tipo_documento,valor)
SELECT ('de110005-0000-4000-8000-' || lpad(d.numero::text,12,'0'))::uuid,
       ('de110001-0000-4000-8000-' || lpad(d.numero::text,12,'0'))::uuid,
       (SELECT id_tipo_documento FROM public.tipo_documento WHERE lower(btrim(nombre))='dui'
        ORDER BY id_tipo_documento='de110004-0000-4000-8000-000000000001' DESC, id_tipo_documento LIMIT 1),
       d.valor
FROM documentos_desarrollo d
ON CONFLICT DO NOTHING;

-- Repetir el script conserva el hash y el estado de las cuentas existentes.
INSERT INTO public.usuario (id_usuario,id_persona,nombre_usuario,password_hash,activo)
SELECT ('de110003-0000-4000-8000-' || lpad(numero::text,12,'0'))::uuid,
       ('de110001-0000-4000-8000-' || lpad(numero::text,12,'0'))::uuid,
       nombre_usuario,k.hash,true
FROM cuentas_desarrollo CROSS JOIN clave_desarrollo k
ON CONFLICT (id_usuario) DO NOTHING;

DO $$ BEGIN
    IF (SELECT count(*) FROM cuentas_desarrollo d
        JOIN public.usuario u ON u.nombre_usuario=d.nombre_usuario AND u.activo=true
        JOIN public.persona_rol a ON a.id_persona=u.id_persona
        JOIN public.rol r ON r.id_rol=a.id_rol AND r.codigo=d.codigo AND r.activo=true
        JOIN public.clinica c ON c.id_clinica=a.id_clinica AND c.activo=true
        WHERE c.id_clinica='de110000-0000-4000-8000-000000000001') <> 3 THEN
        RAISE EXCEPTION 'Las cuentas de demostración existentes no coinciden con los tres perfiles activos; no se sobrescribió ningún dato.';
    END IF;
END $$;

DO $$ BEGIN
    IF (SELECT count(*) FROM documentos_desarrollo d
        JOIN public.documento x ON x.id_persona=('de110001-0000-4000-8000-' || lpad(d.numero::text,12,'0'))::uuid
                               AND x.valor=d.valor
        JOIN public.tipo_documento t ON t.id_tipo_documento=x.id_tipo_documento
                                    AND lower(btrim(t.nombre))='dui' AND t.activo=true) <> 3 THEN
        RAISE EXCEPTION 'Los documentos de demostración no coinciden (tipo DUI activo y números 00000000-1/2/3); no se sobrescribió ningún dato.';
    END IF;
END $$;
COMMIT;
\echo 'Login por DUI: admin.demo 00000000-1 (ADMINISTRACION), doctor.demo 00000000-2 (DOCTOR). paciente.demo 00000000-3 es PACIENTE y debe ser rechazado.'
