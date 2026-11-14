\set ON_ERROR_STOP on
-- Opcional, solo para desarrollo y revisión: datos demo del guion del Primer Parcial
-- (docs/datos-demo-rubrica.md). Catálogos, personas, documentos, contactos, asignaciones y
-- cuentas. Las consultas van en 20261113_datos_demo_rubrica_consultas.sql.
-- Requiere 20261111_base_completa.sql, 20261112_documento_tipo_valor_unico.sql y
-- 20261113_orden_examen_examen.sql. password_hash debe venir de ContrasenaService
-- (scripts/preparar-bd-desarrollo.sh genera el de una clave aleatoria; el login es por documento).
--
-- UUID fijos (prefijo de12, uno por tabla; el sufijo es el número de fila):
--   de120000 clínicas            de120001 roles nuevos           de120002 tipos de medio de contacto
--   de120003 tipos de documento  de120004 tipos de examen        de120005 exámenes
--   de120006 clasificaciones     de120007 procedimientos         de120008 pasos
--   de120009 secuencias          de12000a paso-examen            de12000b personas
--   de12000c documentos          de12000d medios de contacto     de12000e persona_rol
--   de12000f usuarios
-- Los roles ADMINISTRACION, DOCTOR y PACIENTE de base_completa se reutilizan por código;
-- ADMINISTRACION pasa a llamarse "Atención al público". Los tipos de documento y de medio de
-- contacto se reutilizan si ya existe uno con el mismo nombre.
--
-- Idempotente: repetirlo deja los catálogos y las personas demo exactamente como en el archivo
-- (ON CONFLICT ... DO UPDATE). Las cuentas existentes conservan su hash. Todos los datos son
-- inventados.
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
    IF (SELECT count(*) FROM public.rol WHERE codigo IN ('ADMINISTRACION','DOCTOR','PACIENTE')) <> 3 THEN
        RAISE EXCEPTION 'Faltan los roles ADMINISTRACION, DOCTOR y PACIENTE. Aplique primero 20261111_base_completa.sql.';
    END IF;
END $$;

-- 1. Clínicas: dos activas y una inactiva, con tipos distintos.
INSERT INTO public.clinica (id_clinica, nombre, activo, tipo, comentarios)
VALUES ('de120000-0000-4000-8000-000000000001', 'Clínica Santa Ana', true, 'General', 'Datos demo del guion.'),
       ('de120000-0000-4000-8000-000000000002', 'Clínica San Salvador', true, 'Especialidades', 'Datos demo del guion.'),
       ('de120000-0000-4000-8000-000000000003', 'Clínica Sonsonate', false, 'Pediátrica', 'Datos demo del guion.')
ON CONFLICT (id_clinica) DO UPDATE SET nombre = EXCLUDED.nombre, activo = EXCLUDED.activo,
    tipo = EXCLUDED.tipo, comentarios = EXCLUDED.comentarios;

-- 2. Roles: cuatro activos y uno inactivo. Empleado es cualquier rol activo excepto PACIENTE.
CREATE TEMP TABLE demo_rol (codigo text PRIMARY KEY, nombre text, activo boolean, id_fijo uuid) ON COMMIT DROP;
INSERT INTO demo_rol VALUES
    ('ADMINISTRACION', 'Atención al público', true, NULL),
    ('PACIENTE', 'Paciente', true, NULL),
    ('ENFERMERA', 'Enfermera', true, 'de120001-0000-4000-8000-000000000001'),
    ('DOCTOR', 'Doctor', true, NULL),
    ('FARMACEUTICO', 'Farmacéutico', false, 'de120001-0000-4000-8000-000000000002');
INSERT INTO public.rol (id_rol, nombre, activo, codigo, observaciones)
SELECT d.id_fijo, d.nombre, d.activo, d.codigo, 'Rol demo del guion.'
FROM demo_rol d
WHERE d.id_fijo IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.rol r WHERE r.codigo = d.codigo)
ON CONFLICT (id_rol) DO NOTHING;
UPDATE public.rol r SET nombre = d.nombre, activo = d.activo FROM demo_rol d WHERE r.codigo = d.codigo;

-- 3 y 4. Tipos de medio de contacto y de documento: dos activos con expresión regular y uno
-- inactivo. No hay columna de formato: el formato va en indicaciones.
CREATE TEMP TABLE demo_tipo (tabla text, n integer, nombre text, activo boolean, expresion text, indicaciones text,
    id uuid, PRIMARY KEY (tabla, n)) ON COMMIT DROP;
INSERT INTO demo_tipo (tabla, n, nombre, activo, expresion, indicaciones) VALUES
    ('medio', 1, 'Teléfono', true, '^[267][0-9]{3}-[0-9]{4}$',
     '4 dígitos, guion y 4 dígitos; empieza con 2, 6 o 7. Ejemplo: 7123-4567'),
    ('medio', 2, 'Correo electrónico', true, '^[^@\s]+@[^@\s]+\.[A-Za-z]{2,}$',
     'Formato texto@dominio.ext. Ejemplo: nombre@correo.com'),
    ('medio', 3, 'Fax', false, '^[0-9]{4}-[0-9]{4}$', '4 dígitos, guion y 4 dígitos. Ejemplo: 2222-3333'),
    ('documento', 1, 'DUI', true, '^[0-9]{8}-[0-9]$',
     'Ocho dígitos, guion y dígito verificador. Ejemplo: 01000001-1'),
    ('documento', 2, 'Pasaporte', true, '^[A-Z][0-9]{8}$',
     'Una letra mayúscula y ocho dígitos. Ejemplo: A10000001'),
    ('documento', 3, 'Licencia de conducir', false, '^[0-9]{4}-[0-9]{6}-[0-9]{3}-[0-9]$',
     '4, 6, 3 y 1 dígitos separados por guiones. Ejemplo: 0101-010101-101-1');
UPDATE demo_tipo d SET id = coalesce(
    (SELECT t.id_tipo_medio_contacto FROM public.tipo_medio_contacto t WHERE lower(btrim(t.nombre)) = lower(d.nombre)
     ORDER BY t.id_tipo_medio_contacto = ('de120002-0000-4000-8000-' || lpad(d.n::text, 12, '0'))::uuid DESC, t.id_tipo_medio_contacto LIMIT 1),
    ('de120002-0000-4000-8000-' || lpad(d.n::text, 12, '0'))::uuid)
WHERE d.tabla = 'medio';
UPDATE demo_tipo d SET id = coalesce(
    (SELECT t.id_tipo_documento FROM public.tipo_documento t WHERE lower(btrim(t.nombre)) = lower(d.nombre)
     ORDER BY t.id_tipo_documento = ('de120003-0000-4000-8000-' || lpad(d.n::text, 12, '0'))::uuid DESC, t.id_tipo_documento LIMIT 1),
    ('de120003-0000-4000-8000-' || lpad(d.n::text, 12, '0'))::uuid)
WHERE d.tabla = 'documento';
INSERT INTO public.tipo_medio_contacto (id_tipo_medio_contacto, nombre, indicaciones, expresion_regular, activo)
SELECT id, nombre, indicaciones, expresion, activo FROM demo_tipo WHERE tabla = 'medio'
ON CONFLICT (id_tipo_medio_contacto) DO UPDATE SET nombre = EXCLUDED.nombre, indicaciones = EXCLUDED.indicaciones,
    expresion_regular = EXCLUDED.expresion_regular, activo = EXCLUDED.activo;
INSERT INTO public.tipo_documento (id_tipo_documento, nombre, indicaciones, expresion_regular, activo)
SELECT id, nombre, indicaciones, expresion, activo FROM demo_tipo WHERE tabla = 'documento'
ON CONFLICT (id_tipo_documento) DO UPDATE SET nombre = EXCLUDED.nombre, indicaciones = EXCLUDED.indicaciones,
    expresion_regular = EXCLUDED.expresion_regular, activo = EXCLUDED.activo;

-- 5. Tipos de examen: dos activos y uno inactivo.
INSERT INTO public.tipo_examen (id_tipo_examen, nombre, activo, observaciones)
VALUES ('de120004-0000-4000-8000-000000000001', 'Laboratorio clínico', true, 'Tipo de examen demo.'),
       ('de120004-0000-4000-8000-000000000002', 'Imagenología', true, 'Tipo de examen demo.'),
       ('de120004-0000-4000-8000-000000000003', 'Patología', false, 'Tipo de examen demo.')
ON CONFLICT (id_tipo_examen) DO UPDATE SET nombre = EXCLUDED.nombre, activo = EXCLUDED.activo, observaciones = EXCLUDED.observaciones;

-- 6. Exámenes: cuatro activos y uno inactivo, cada uno clasificado en un tipo.
CREATE TEMP TABLE demo_examen (n integer PRIMARY KEY, nombre text, activo boolean, tipo integer) ON COMMIT DROP;
INSERT INTO demo_examen VALUES
    (1, 'Hemograma completo', true, 1), (2, 'Glucosa en ayunas', true, 1), (3, 'Examen general de orina', true, 1),
    (4, 'Rayos X de tórax', true, 2), (5, 'Electrocardiograma', false, 2);
INSERT INTO public.examen (id_examen, nombre, activo, observaciones)
SELECT ('de120005-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid, nombre, activo, 'Examen demo.' FROM demo_examen
ON CONFLICT (id_examen) DO UPDATE SET nombre = EXCLUDED.nombre, activo = EXCLUDED.activo, observaciones = EXCLUDED.observaciones;
INSERT INTO public.examen_tipo_examen (id_examen_tipo_examen, id_examen, id_tipo_examen, fecha_creacion)
SELECT ('de120006-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid, ('de120005-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
       ('de120004-0000-4000-8000-' || lpad(tipo::text, 12, '0'))::uuid, '2026-01-01T00:00:00Z'
FROM demo_examen
ON CONFLICT (id_examen_tipo_examen) DO UPDATE SET id_examen = EXCLUDED.id_examen, id_tipo_examen = EXCLUDED.id_tipo_examen;

-- 7. Procedimientos: dos activos de tres pasos (inicial → siguiente → finalización), cada paso
-- con un rol distinto. El orden de los pasos lo dan las secuencias SIGUIENTE.
INSERT INTO public.procedimiento (id_procedimiento, nombre, activo, observaciones)
VALUES ('de120007-0000-4000-8000-000000000001', 'Consulta general', true, 'Procedimiento demo del guion.'),
       ('de120007-0000-4000-8000-000000000002', 'Control de hipertensión', true, 'Procedimiento demo del guion.')
ON CONFLICT (id_procedimiento) DO UPDATE SET nombre = EXCLUDED.nombre, activo = EXCLUDED.activo, observaciones = EXCLUDED.observaciones;

CREATE TEMP TABLE demo_paso_def (n integer PRIMARY KEY, procedimiento integer, nombre text, codigo text, fin boolean) ON COMMIT DROP;
INSERT INTO demo_paso_def VALUES
    (1, 1, 'Triaje', 'ENFERMERA', false), (2, 1, 'Consulta médica', 'DOCTOR', false),
    (3, 1, 'Entrega de indicaciones', 'ADMINISTRACION', true),
    (4, 2, 'Toma de presión arterial', 'ENFERMERA', false), (5, 2, 'Evaluación médica', 'DOCTOR', false),
    (6, 2, 'Programación de seguimiento', 'ADMINISTRACION', true);
INSERT INTO public.procedimiento_paso (id_procedimiento_paso, id_procedimiento, nombre, indica_fin, id_rol)
SELECT ('de120008-0000-4000-8000-' || lpad(d.n::text, 12, '0'))::uuid, ('de120007-0000-4000-8000-' || lpad(d.procedimiento::text, 12, '0'))::uuid,
       d.nombre, d.fin, r.id_rol
FROM demo_paso_def d JOIN public.rol r ON r.codigo = d.codigo
ON CONFLICT (id_procedimiento_paso) DO UPDATE SET id_procedimiento = EXCLUDED.id_procedimiento, nombre = EXCLUDED.nombre,
    indica_fin = EXCLUDED.indica_fin, id_rol = EXCLUDED.id_rol;
INSERT INTO public.procedimiento_paso_secuencia (id_procedimiento_paso_secuencia, id_procedimiento_paso, id_procedimiento_paso_referencia, tipo_secuencia)
SELECT ('de120009-0000-4000-8000-' || lpad(s.n::text, 12, '0'))::uuid, ('de120008-0000-4000-8000-' || lpad(s.desde::text, 12, '0'))::uuid,
       ('de120008-0000-4000-8000-' || lpad(s.hasta::text, 12, '0'))::uuid, 'SIGUIENTE'
FROM (VALUES (1, 1, 2), (2, 2, 3), (3, 4, 5), (4, 5, 6)) AS s(n, desde, hasta)
ON CONFLICT (id_procedimiento_paso_secuencia) DO UPDATE SET id_procedimiento_paso = EXCLUDED.id_procedimiento_paso,
    id_procedimiento_paso_referencia = EXCLUDED.id_procedimiento_paso_referencia, tipo_secuencia = EXCLUDED.tipo_secuencia;

-- Exámenes asignables: Consulta médica los cinco (Electrocardiograma está inactivo y se filtra);
-- Evaluación médica Glucosa en ayunas y Rayos X de tórax.
INSERT INTO public.procedimiento_paso_examen (id_procedimiento_paso_examen, id_procedimiento_paso, id_examen, activo, observaciones, fecha_creacion)
SELECT ('de12000a-0000-4000-8000-' || lpad(a.n::text, 12, '0'))::uuid, ('de120008-0000-4000-8000-' || lpad(a.paso::text, 12, '0'))::uuid,
       ('de120005-0000-4000-8000-' || lpad(a.examen::text, 12, '0'))::uuid, true, 'Asociación demo.', '2026-01-01T00:00:00Z'
FROM (VALUES (1, 2, 1), (2, 2, 2), (3, 2, 3), (4, 2, 4), (5, 2, 5), (6, 5, 2), (7, 5, 4)) AS a(n, paso, examen)
ON CONFLICT (id_procedimiento_paso_examen) DO UPDATE SET id_procedimiento_paso = EXCLUDED.id_procedimiento_paso,
    id_examen = EXCLUDED.id_examen, activo = EXCLUDED.activo, observaciones = EXCLUDED.observaciones;

-- 8. Personas, con fechas de nacimiento fijas (día local de El Salvador), teléfono y correo.
CREATE TEMP TABLE demo_persona (n integer PRIMARY KEY, nombres text, apellidos text, nacimiento date, telefono text, correo text,
    usuario text) ON COMMIT DROP;
INSERT INTO demo_persona VALUES
    (1, 'Carlos', 'Gómez', '1980-03-15', '7100-0001', 'carlos.gomez@galenosv.test', 'carlos.gomez'),
    (2, 'Lucía', 'Pérez', '1988-07-22', '7100-0002', 'lucia.perez@galenosv.test', 'lucia.perez'),
    (3, 'Marta', 'López', '1990-11-05', '7100-0003', 'marta.lopez@galenosv.test', 'marta.lopez'),
    (4, 'Ana', 'Martínez', '1992-04-10', '7100-0004', 'ana.martinez@galenosv.test', NULL),
    (5, 'Pedro', 'Hernández', '1975-01-30', '7100-0005', 'pedro.hernandez@galenosv.test', NULL),
    (6, 'Rosa', 'Méndez', '1985-09-18', '7100-0006', 'rosa.mendez@galenosv.test', 'rosa.mendez'),
    (7, 'Jorge', 'Ruiz', '1978-12-02', '7100-0007', 'jorge.ruiz@galenosv.test', 'jorge.ruiz'),
    (8, 'Julia', 'Flores', '1960-06-25', '7100-0008', 'julia.flores@galenosv.test', NULL),
    (9, 'Juan', 'Vásquez', '2001-02-14', '7100-0009', 'juan.vasquez@galenosv.test', NULL);
INSERT INTO public.persona (id_persona, nombres, apellidos, fecha_nacimiento, fecha_creacion)
SELECT ('de12000b-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid, nombres, apellidos,
       nacimiento::timestamp AT TIME ZONE 'America/El_Salvador', '2026-01-01T00:00:00Z'
FROM demo_persona
ON CONFLICT (id_persona) DO UPDATE SET nombres = EXCLUDED.nombres, apellidos = EXCLUDED.apellidos,
    fecha_nacimiento = EXCLUDED.fecha_nacimiento;

-- Documentos: número único dentro del tipo. Si otra persona ya tiene uno de estos números, la
-- restricción uq_documento_tipo_valor detiene el script sin guardar nada.
CREATE TEMP TABLE demo_documento (n integer PRIMARY KEY, persona integer, tipo text, valor text) ON COMMIT DROP;
INSERT INTO demo_documento VALUES
    (1, 1, 'DUI', '01000001-1'), (2, 1, 'Pasaporte', 'A10000001'), (3, 2, 'DUI', '01000002-2'),
    (4, 3, 'DUI', '01000003-3'), (5, 4, 'DUI', '01000004-4'), (6, 4, 'Pasaporte', 'B10000004'),
    (7, 5, 'DUI', '01000005-5'), (8, 6, 'DUI', '01000006-6'), (9, 7, 'DUI', '01000007-7'),
    (10, 8, 'DUI', '01000008-8'), (11, 9, 'DUI', '01000009-9');
INSERT INTO public.documento (id_documento, id_persona, id_tipo_documento, valor)
SELECT ('de12000c-0000-4000-8000-' || lpad(d.n::text, 12, '0'))::uuid, ('de12000b-0000-4000-8000-' || lpad(d.persona::text, 12, '0'))::uuid,
       t.id, d.valor
FROM demo_documento d JOIN demo_tipo t ON t.tabla = 'documento' AND t.nombre = d.tipo
ON CONFLICT (id_documento) DO UPDATE SET id_persona = EXCLUDED.id_persona, id_tipo_documento = EXCLUDED.id_tipo_documento,
    valor = EXCLUDED.valor;

INSERT INTO public.medio_contacto (id_medio_contacto, id_persona, id_tipo_medio_contacto, valor, fecha_creacion)
SELECT ('de12000d-0000-4000-8000-' || lpad((p.n * 2 - 1 + m.desfase)::text, 12, '0'))::uuid,
       ('de12000b-0000-4000-8000-' || lpad(p.n::text, 12, '0'))::uuid, t.id,
       CASE WHEN m.desfase = 0 THEN p.telefono ELSE p.correo END, '2026-01-01T00:00:00Z'
FROM demo_persona p
CROSS JOIN (VALUES (0, 'Teléfono'), (1, 'Correo electrónico')) AS m(desfase, tipo)
JOIN demo_tipo t ON t.tabla = 'medio' AND t.nombre = m.tipo
ON CONFLICT (id_medio_contacto) DO UPDATE SET id_persona = EXCLUDED.id_persona,
    id_tipo_medio_contacto = EXCLUDED.id_tipo_medio_contacto, valor = EXCLUDED.valor;

-- Asignaciones (rol y clínica). fecha_creacion fija: la asignación más antigua de cada empleado
-- es su rol inicial al iniciar sesión (Carlos: Doctor en Santa Ana). Pedro no tiene ninguna.
CREATE TEMP TABLE demo_asignacion (n integer PRIMARY KEY, persona integer, codigo text, clinica integer) ON COMMIT DROP;
INSERT INTO demo_asignacion VALUES
    (1, 1, 'DOCTOR', 1), (2, 1, 'DOCTOR', 2), (3, 2, 'ENFERMERA', 1), (4, 3, 'ADMINISTRACION', 1),
    (5, 3, 'ADMINISTRACION', 2), (6, 4, 'PACIENTE', 1), (7, 4, 'PACIENTE', 2), (8, 6, 'ENFERMERA', 2),
    (9, 7, 'DOCTOR', 2), (10, 8, 'PACIENTE', 1), (11, 9, 'PACIENTE', 1);
INSERT INTO public.persona_rol (id_persona_rol, id_persona, id_rol, id_clinica, fecha_creacion)
SELECT ('de12000e-0000-4000-8000-' || lpad(a.n::text, 12, '0'))::uuid, ('de12000b-0000-4000-8000-' || lpad(a.persona::text, 12, '0'))::uuid,
       r.id_rol, ('de120000-0000-4000-8000-' || lpad(a.clinica::text, 12, '0'))::uuid,
       '2026-01-01T00:00:00Z'::timestamptz + a.n * interval '1 minute'
FROM demo_asignacion a JOIN public.rol r ON r.codigo = a.codigo
ON CONFLICT (id_persona_rol) DO UPDATE SET id_persona = EXCLUDED.id_persona, id_rol = EXCLUDED.id_rol,
    id_clinica = EXCLUDED.id_clinica, fecha_creacion = EXCLUDED.fecha_creacion;

-- Cuentas de los cinco empleados. Repetir el script conserva el hash; el estado vuelve a activo.
INSERT INTO public.usuario (id_usuario, id_persona, nombre_usuario, correo, password_hash, activo)
SELECT ('de12000f-0000-4000-8000-' || lpad(p.n::text, 12, '0'))::uuid, ('de12000b-0000-4000-8000-' || lpad(p.n::text, 12, '0'))::uuid,
       p.usuario, p.correo, k.hash, true
FROM demo_persona p CROSS JOIN clave_desarrollo k
WHERE p.usuario IS NOT NULL
ON CONFLICT (id_usuario) DO UPDATE SET id_persona = EXCLUDED.id_persona, nombre_usuario = EXCLUDED.nombre_usuario,
    correo = EXCLUDED.correo, activo = true;

-- Verificación: si algo no quedó como en el archivo, no se guarda nada.
DO $$ BEGIN
    IF (SELECT count(*) FROM public.clinica WHERE id_clinica::text LIKE 'de120000-%') <> 3
       OR (SELECT count(*) FROM public.rol WHERE codigo IN ('ADMINISTRACION','PACIENTE','ENFERMERA','DOCTOR') AND activo) <> 4
       OR (SELECT count(*) FROM public.rol WHERE codigo = 'FARMACEUTICO' AND NOT activo) <> 1
       OR (SELECT count(*) FROM public.examen WHERE id_examen::text LIKE 'de120005-%') <> 5
       OR (SELECT count(*) FROM public.procedimiento_paso WHERE id_procedimiento_paso::text LIKE 'de120008-%') <> 6
       OR (SELECT count(*) FROM public.persona WHERE id_persona::text LIKE 'de12000b-%') <> 9
       OR (SELECT count(*) FROM public.documento WHERE id_documento::text LIKE 'de12000c-%') <> 11
       OR (SELECT count(*) FROM public.medio_contacto WHERE id_medio_contacto::text LIKE 'de12000d-%') <> 18
       OR (SELECT count(*) FROM public.persona_rol WHERE id_persona_rol::text LIKE 'de12000e-%') <> 11
       OR (SELECT count(*) FROM public.usuario WHERE id_usuario::text LIKE 'de12000f-%' AND activo) <> 5 THEN
        RAISE EXCEPTION 'Los datos demo del guion quedaron incompletos; no se guardó ningún cambio.';
    END IF;
END $$;
COMMIT;
\echo 'Datos demo del guion: 3 clínicas, 5 roles, 9 personas, 5 cuentas de empleado. Login por DUI: 01000001-1 (Carlos, Doctor), 01000002-2 (Lucía, Enfermera), 01000003-3 (Marta, Atención al público).'
