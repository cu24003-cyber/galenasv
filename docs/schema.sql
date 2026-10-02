-- Esquema de GalenoSV para una base PostgreSQL vacía.
-- Tablas, tipos e índices contrastados con pg_dump --schema-only (20 tablas).
-- Las 24 FK corresponden a las entidades JPA y usan ON UPDATE CASCADE / ON DELETE RESTRICT.
-- Incluye id_procedimiento_paso e id_medico_rol; no ejecute las migraciones otra vez.


CREATE TABLE clinica (
    id_clinica uuid NOT NULL,
    nombre character varying(255) NOT NULL,
    activo boolean,
    tipo character varying(20),
    comentarios text
);

CREATE TABLE consulta (
    id_consulta uuid NOT NULL,
    fecha_inicio timestamp with time zone DEFAULT now(),
    fecha_fin timestamp with time zone,
    referencia_externa text,
    observaciones text,
    id_persona_rol uuid,
    id_medico_rol uuid
);

CREATE TABLE consulta_procedimiento (
    id_consulta_procedimiento uuid NOT NULL,
    id_consulta uuid,
    id_procedimiento uuid,
    fecha_inicio timestamp with time zone,
    fecha_fin timestamp with time zone,
    observaciones text
);

CREATE TABLE consulta_procedimiento_paso (
    id_consulta_procedimiento_paso uuid CONSTRAINT consulta_procedimiento_paso_id_consulta_procedimiento__not_null NOT NULL,
    id_consulta_procedimiento uuid,
    id_persona_rol uuid,
    fecha_inicio timestamp with time zone DEFAULT now(),
    fecha_fin timestamp with time zone,
    estado character varying(20),
    id_procedimiento_paso uuid
);

CREATE TABLE documento (
    id_documento uuid NOT NULL,
    id_persona uuid,
    id_tipo_documento uuid,
    valor text,
    ruta_fisica text
);

CREATE TABLE examen (
    id_examen uuid NOT NULL,
    nombre character varying(255),
    activo boolean DEFAULT true,
    observaciones text
);

CREATE TABLE examen_resultado (
    id_examen_resultado uuid NOT NULL,
    id_orden_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    resultado text,
    interpretacion text,
    ruta_atestado text
);

CREATE TABLE examen_tipo_examen (
    id_examen_tipo_examen uuid NOT NULL,
    id_examen uuid,
    id_tipo_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    observaciones text
);

CREATE TABLE medio_contacto (
    id_medio_contacto uuid NOT NULL,
    id_persona uuid,
    id_tipo_medio_contacto uuid,
    valor text,
    fecha_creacion timestamp with time zone DEFAULT now()
);

CREATE TABLE orden_examen (
    id_orden_examen uuid NOT NULL,
    id_consulta_procedimiento_paso uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    indicaciones text
);

CREATE TABLE persona (
    id_persona uuid NOT NULL,
    nombres character varying(255),
    apellidos character varying(255),
    fecha_nacimiento timestamp with time zone,
    fecha_creacion timestamp with time zone DEFAULT now()
);

CREATE TABLE persona_rol (
    id_persona_rol uuid NOT NULL,
    id_persona uuid,
    id_rol uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    id_clinica uuid
);

CREATE TABLE procedimiento (
    id_procedimiento uuid NOT NULL,
    nombre character varying(155),
    activo boolean,
    observaciones text
);

CREATE TABLE procedimiento_paso (
    id_procedimiento_paso uuid NOT NULL,
    id_procedimiento uuid,
    nombre character varying(155),
    indica_fin boolean DEFAULT false,
    id_rol uuid
);

CREATE TABLE procedimiento_paso_examen (
    id_procedimiento_paso_examen uuid NOT NULL,
    id_procedimiento_paso uuid,
    id_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    activo boolean DEFAULT true,
    observaciones text
);

CREATE TABLE procedimiento_paso_secuencia (
    id_procedimiento_paso_secuencia uuid CONSTRAINT procedimiento_paso_secuenci_id_procedimiento_paso_secu_not_null NOT NULL,
    id_procedimiento_paso uuid,
    id_procedimiento_paso_referencia uuid,
    tipo_secuencia character varying(20)
);

CREATE TABLE rol (
    id_rol uuid NOT NULL,
    nombre character varying(155),
    activo boolean,
    observaciones text
);

CREATE TABLE tipo_documento (
    id_tipo_documento uuid NOT NULL,
    nombre character varying(155),
    indicaciones text,
    expresion_regular text DEFAULT '.'::text,
    activo boolean
);

CREATE TABLE tipo_examen (
    id_tipo_examen uuid NOT NULL,
    nombre character varying,
    activo boolean,
    observaciones text
);

CREATE TABLE tipo_medio_contacto (
    id_tipo_medio_contacto uuid NOT NULL,
    nombre character varying(155),
    indicaciones text,
    expresion_regular text,
    activo boolean
);

ALTER TABLE ONLY clinica
    ADD CONSTRAINT pk_clinica PRIMARY KEY (id_clinica);

ALTER TABLE ONLY consulta
    ADD CONSTRAINT pk_consulta PRIMARY KEY (id_consulta);

ALTER TABLE ONLY consulta_procedimiento
    ADD CONSTRAINT pk_consulta_procedimiento PRIMARY KEY (id_consulta_procedimiento);

ALTER TABLE ONLY consulta_procedimiento_paso
    ADD CONSTRAINT pk_consulta_procedimiento_paso PRIMARY KEY (id_consulta_procedimiento_paso);

ALTER TABLE ONLY documento
    ADD CONSTRAINT pk_documento PRIMARY KEY (id_documento);

ALTER TABLE ONLY examen
    ADD CONSTRAINT pk_examen PRIMARY KEY (id_examen);

ALTER TABLE ONLY examen_resultado
    ADD CONSTRAINT pk_examen_resultado PRIMARY KEY (id_examen_resultado);

ALTER TABLE ONLY examen_tipo_examen
    ADD CONSTRAINT pk_examen_tipo_examen PRIMARY KEY (id_examen_tipo_examen);

ALTER TABLE ONLY medio_contacto
    ADD CONSTRAINT pk_medio_contacto PRIMARY KEY (id_medio_contacto);

ALTER TABLE ONLY orden_examen
    ADD CONSTRAINT pk_orden_examen PRIMARY KEY (id_orden_examen);

ALTER TABLE ONLY persona
    ADD CONSTRAINT pk_persona PRIMARY KEY (id_persona);

ALTER TABLE ONLY persona_rol
    ADD CONSTRAINT pk_persona_rol PRIMARY KEY (id_persona_rol);

ALTER TABLE ONLY procedimiento
    ADD CONSTRAINT pk_procedimiento PRIMARY KEY (id_procedimiento);

ALTER TABLE ONLY procedimiento_paso
    ADD CONSTRAINT pk_procedimiento_paso PRIMARY KEY (id_procedimiento_paso);

ALTER TABLE ONLY procedimiento_paso_examen
    ADD CONSTRAINT pk_procedimiento_paso_examen PRIMARY KEY (id_procedimiento_paso_examen);

ALTER TABLE ONLY procedimiento_paso_secuencia
    ADD CONSTRAINT pk_procedimiento_paso_secuencia PRIMARY KEY (id_procedimiento_paso_secuencia);

ALTER TABLE ONLY rol
    ADD CONSTRAINT pk_rol PRIMARY KEY (id_rol);

ALTER TABLE ONLY tipo_documento
    ADD CONSTRAINT pk_tipo_documento PRIMARY KEY (id_tipo_documento);

ALTER TABLE ONLY tipo_examen
    ADD CONSTRAINT pk_tipo_examen PRIMARY KEY (id_tipo_examen);

ALTER TABLE ONLY tipo_medio_contacto
    ADD CONSTRAINT pk_tipo_medio_contacto PRIMARY KEY (id_tipo_medio_contacto);

CREATE INDEX fki_fk_persona_rol_clinica ON persona_rol USING btree (id_clinica);

CREATE INDEX idx_consulta_abierta_paciente ON consulta USING btree (id_persona_rol) WHERE (fecha_fin IS NULL);

CREATE INDEX idx_consulta_medico_clinica ON consulta USING btree (id_medico_rol, fecha_inicio DESC);

CREATE INDEX idx_consulta_paso_procedimiento ON consulta_procedimiento_paso USING btree (id_consulta_procedimiento);

CREATE INDEX idx_consulta_procedimiento_consulta ON consulta_procedimiento USING btree (id_consulta);

ALTER TABLE consulta ADD CONSTRAINT fk_consulta_id_persona_rol FOREIGN KEY (id_persona_rol) REFERENCES persona_rol (id_persona_rol) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE consulta ADD CONSTRAINT fk_consulta_id_medico_rol FOREIGN KEY (id_medico_rol) REFERENCES persona_rol (id_persona_rol) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE consulta_procedimiento ADD CONSTRAINT fk_consulta_procedimiento_id_procedimiento FOREIGN KEY (id_procedimiento) REFERENCES procedimiento (id_procedimiento) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE consulta_procedimiento ADD CONSTRAINT fk_consulta_procedimiento_id_consulta FOREIGN KEY (id_consulta) REFERENCES consulta (id_consulta) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE consulta_procedimiento_paso ADD CONSTRAINT fk_consulta_procedimiento_paso_id_consulta_procedimiento FOREIGN KEY (id_consulta_procedimiento) REFERENCES consulta_procedimiento (id_consulta_procedimiento) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE consulta_procedimiento_paso ADD CONSTRAINT fk_consulta_procedimiento_paso_id_persona_rol FOREIGN KEY (id_persona_rol) REFERENCES persona_rol (id_persona_rol) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE consulta_procedimiento_paso ADD CONSTRAINT fk_consulta_procedimiento_paso_id_procedimiento_paso FOREIGN KEY (id_procedimiento_paso) REFERENCES procedimiento_paso (id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE documento ADD CONSTRAINT fk_documento_id_persona FOREIGN KEY (id_persona) REFERENCES persona (id_persona) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE documento ADD CONSTRAINT fk_documento_id_tipo_documento FOREIGN KEY (id_tipo_documento) REFERENCES tipo_documento (id_tipo_documento) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE examen_resultado ADD CONSTRAINT fk_examen_resultado_id_orden_examen FOREIGN KEY (id_orden_examen) REFERENCES orden_examen (id_orden_examen) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE examen_tipo_examen ADD CONSTRAINT fk_examen_tipo_examen_id_examen FOREIGN KEY (id_examen) REFERENCES examen (id_examen) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE examen_tipo_examen ADD CONSTRAINT fk_examen_tipo_examen_id_tipo_examen FOREIGN KEY (id_tipo_examen) REFERENCES tipo_examen (id_tipo_examen) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE medio_contacto ADD CONSTRAINT fk_medio_contacto_id_persona FOREIGN KEY (id_persona) REFERENCES persona (id_persona) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE medio_contacto ADD CONSTRAINT fk_medio_contacto_id_tipo_medio_contacto FOREIGN KEY (id_tipo_medio_contacto) REFERENCES tipo_medio_contacto (id_tipo_medio_contacto) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE orden_examen ADD CONSTRAINT fk_orden_examen_id_consulta_procedimiento_paso FOREIGN KEY (id_consulta_procedimiento_paso) REFERENCES consulta_procedimiento_paso (id_consulta_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE persona_rol ADD CONSTRAINT fk_persona_rol_id_clinica FOREIGN KEY (id_clinica) REFERENCES clinica (id_clinica) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE persona_rol ADD CONSTRAINT fk_persona_rol_id_persona FOREIGN KEY (id_persona) REFERENCES persona (id_persona) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE persona_rol ADD CONSTRAINT fk_persona_rol_id_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE procedimiento_paso ADD CONSTRAINT fk_procedimiento_paso_id_procedimiento FOREIGN KEY (id_procedimiento) REFERENCES procedimiento (id_procedimiento) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE procedimiento_paso ADD CONSTRAINT fk_procedimiento_paso_id_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE procedimiento_paso_examen ADD CONSTRAINT fk_procedimiento_paso_examen_id_examen FOREIGN KEY (id_examen) REFERENCES examen (id_examen) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE procedimiento_paso_examen ADD CONSTRAINT fk_procedimiento_paso_examen_id_procedimiento_paso FOREIGN KEY (id_procedimiento_paso) REFERENCES procedimiento_paso (id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE procedimiento_paso_secuencia ADD CONSTRAINT fk_procedimiento_paso_secuencia_id_procedimiento_paso_referencia FOREIGN KEY (id_procedimiento_paso_referencia) REFERENCES procedimiento_paso (id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE procedimiento_paso_secuencia ADD CONSTRAINT fk_procedimiento_paso_secuencia_id_procedimiento_paso FOREIGN KEY (id_procedimiento_paso) REFERENCES procedimiento_paso (id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT;
