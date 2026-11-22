CREATE TABLE public.clinica (
    id_clinica uuid NOT NULL,
    nombre character varying(255) NOT NULL,
    activo boolean,
    tipo character varying(20),
    comentarios text
);


--
-- TOC entry 225 (class 1259 OID 18316)
-- Name: consulta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.consulta (
    id_consulta uuid NOT NULL,
    fecha_inicio timestamp with time zone DEFAULT now(),
    fecha_fin timestamp with time zone,
    referencia_externa text,
    observaciones text,
    id_persona_rol uuid
);


--
-- TOC entry 229 (class 1259 OID 18367)
-- Name: consulta_procedimiento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.consulta_procedimiento (
    id_consulta_procedimiento uuid NOT NULL,
    id_consulta uuid,
    id_procedimiento uuid,
    fecha_inicio timestamp with time zone,
    fecha_fin timestamp with time zone,
    observaciones text
);


--
-- TOC entry 230 (class 1259 OID 18379)
-- Name: consulta_procedimiento_paso; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.consulta_procedimiento_paso (
    id_consulta_procedimiento_paso uuid NOT NULL,
    id_consulta_procedimiento uuid,
    id_persona_rol uuid,
    fecha_inicio timestamp with time zone DEFAULT now(),
    fecha_fin timestamp with time zone,
    estado character varying(20)
);


--
-- TOC entry 221 (class 1259 OID 18247)
-- Name: documento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documento (
    id_documento uuid NOT NULL,
    id_persona uuid,
    id_tipo_documento uuid,
    valor text,
    ruta_fisica text
);


--
-- TOC entry 232 (class 1259 OID 18407)
-- Name: examen; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.examen (
    id_examen uuid NOT NULL,
    nombre character varying(255),
    activo boolean DEFAULT true,
    observaciones text
);


--
-- TOC entry 236 (class 1259 OID 18481)
-- Name: examen_resultado; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.examen_resultado (
    id_examen_resultado uuid NOT NULL,
    id_orden_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    resultado text,
    interpretacion text,
    ruta_atestado text
);


--
-- TOC entry 233 (class 1259 OID 18415)
-- Name: examen_tipo_examen; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.examen_tipo_examen (
    id_examen_tipo_examen uuid NOT NULL,
    id_examen uuid,
    id_tipo_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    observaciones text
);


--
-- TOC entry 220 (class 1259 OID 18229)
-- Name: medio_contacto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medio_contacto (
    id_medio_contacto uuid NOT NULL,
    id_persona uuid,
    id_tipo_medio_contacto uuid,
    valor text,
    fecha_creacion timestamp with time zone DEFAULT now()
);


--
-- TOC entry 235 (class 1259 OID 18453)
-- Name: orden_examen; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orden_examen (
    id_orden_examen uuid NOT NULL,
    id_consulta_procedimiento_paso uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    indicaciones text
);


--
-- TOC entry 219 (class 1259 OID 18221)
-- Name: persona; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.persona (
    id_persona uuid NOT NULL,
    nombres character varying(255),
    apellidos character varying(255),
    fecha_nacimiento timestamp with time zone,
    fecha_creacion timestamp with time zone DEFAULT now()
);


--
-- TOC entry 223 (class 1259 OID 18271)
-- Name: persona_rol; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.persona_rol (
    id_persona_rol uuid NOT NULL,
    id_persona uuid,
    id_rol uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    id_clinica uuid
);


--
-- TOC entry 226 (class 1259 OID 18334)
-- Name: procedimiento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procedimiento (
    id_procedimiento uuid NOT NULL,
    nombre character varying(155),
    activo boolean,
    observaciones text
);


--
-- TOC entry 227 (class 1259 OID 18341)
-- Name: procedimiento_paso; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procedimiento_paso (
    id_procedimiento_paso uuid NOT NULL,
    id_procedimiento uuid,
    nombre character varying(155),
    indica_fin boolean DEFAULT false,
    id_rol uuid
);


--
-- TOC entry 234 (class 1259 OID 18434)
-- Name: procedimiento_paso_examen; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procedimiento_paso_examen (
    id_procedimiento_paso_examen uuid NOT NULL,
    id_procedimiento_paso uuid,
    id_examen uuid,
    fecha_creacion timestamp with time zone DEFAULT now(),
    activo boolean DEFAULT true,
    observaciones text
);


--
-- TOC entry 228 (class 1259 OID 18352)
-- Name: procedimiento_paso_secuencia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procedimiento_paso_secuencia (
    id_procedimiento_paso_secuencia uuid NOT NULL,
    id_procedimiento_paso uuid,
    id_procedimiento_paso_referencia uuid,
    tipo_secuencia character varying(20)
);


--
-- TOC entry 222 (class 1259 OID 18264)
-- Name: rol; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rol (
    id_rol uuid NOT NULL,
    nombre character varying(155),
    activo boolean,
    observaciones text
);


--
-- TOC entry 218 (class 1259 OID 18213)
-- Name: tipo_documento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_documento (
    id_tipo_documento uuid NOT NULL,
    nombre character varying(155),
    indicaciones text,
    expresion_regular text DEFAULT '.'::text,
    activo boolean
);


--
-- TOC entry 231 (class 1259 OID 18400)
-- Name: tipo_examen; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_examen (
    id_tipo_examen uuid NOT NULL,
    nombre character varying,
    activo boolean,
    observaciones text
);


--
-- TOC entry 217 (class 1259 OID 18206)
-- Name: tipo_medio_contacto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_medio_contacto (
    id_tipo_medio_contacto uuid NOT NULL,
    nombre character varying(155),
    indicaciones text,
    expresion_regular text,
    activo boolean
);


--
-- TOC entry 3572 (class 0 OID 18287)
-- Dependencies: 224
-- Data for Name: clinica; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3573 (class 0 OID 18316)
-- Dependencies: 225
-- Data for Name: consulta; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3577 (class 0 OID 18367)
-- Dependencies: 229
-- Data for Name: consulta_procedimiento; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3578 (class 0 OID 18379)
-- Dependencies: 230
-- Data for Name: consulta_procedimiento_paso; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3569 (class 0 OID 18247)
-- Dependencies: 221
-- Data for Name: documento; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3580 (class 0 OID 18407)
-- Dependencies: 232
-- Data for Name: examen; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3584 (class 0 OID 18481)
-- Dependencies: 236
-- Data for Name: examen_resultado; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3581 (class 0 OID 18415)
-- Dependencies: 233
-- Data for Name: examen_tipo_examen; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3568 (class 0 OID 18229)
-- Dependencies: 220
-- Data for Name: medio_contacto; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3583 (class 0 OID 18453)
-- Dependencies: 235
-- Data for Name: orden_examen; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3567 (class 0 OID 18221)
-- Dependencies: 219
-- Data for Name: persona; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3571 (class 0 OID 18271)
-- Dependencies: 223
-- Data for Name: persona_rol; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3574 (class 0 OID 18334)
-- Dependencies: 226
-- Data for Name: procedimiento; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3575 (class 0 OID 18341)
-- Dependencies: 227
-- Data for Name: procedimiento_paso; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3582 (class 0 OID 18434)
-- Dependencies: 234
-- Data for Name: procedimiento_paso_examen; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3576 (class 0 OID 18352)
-- Dependencies: 228
-- Data for Name: procedimiento_paso_secuencia; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3570 (class 0 OID 18264)
-- Dependencies: 222
-- Data for Name: rol; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3566 (class 0 OID 18213)
-- Dependencies: 218
-- Data for Name: tipo_documento; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3579 (class 0 OID 18400)
-- Dependencies: 231
-- Data for Name: tipo_examen; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3565 (class 0 OID 18206)
-- Dependencies: 217
-- Data for Name: tipo_medio_contacto; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 3375 (class 2606 OID 18293)
-- Name: clinica pk_clinica; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clinica
    ADD CONSTRAINT pk_clinica PRIMARY KEY (id_clinica);


--
-- TOC entry 3377 (class 2606 OID 18323)
-- Name: consulta pk_consulta; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consulta
    ADD CONSTRAINT pk_consulta PRIMARY KEY (id_consulta);


--
-- TOC entry 3385 (class 2606 OID 18373)
-- Name: consulta_procedimiento pk_consulta_procedimiento; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consulta_procedimiento
    ADD CONSTRAINT pk_consulta_procedimiento PRIMARY KEY (id_consulta_procedimiento);


--
-- TOC entry 3387 (class 2606 OID 18384)
-- Name: consulta_procedimiento_paso pk_consulta_procedimiento_paso; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consulta_procedimiento_paso
    ADD CONSTRAINT pk_consulta_procedimiento_paso PRIMARY KEY (id_consulta_procedimiento_paso);


--
-- TOC entry 3368 (class 2606 OID 18253)
-- Name: documento pk_documento; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documento
    ADD CONSTRAINT pk_documento PRIMARY KEY (id_documento);


--
-- TOC entry 3391 (class 2606 OID 18414)
-- Name: examen pk_examen; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.examen
    ADD CONSTRAINT pk_examen PRIMARY KEY (id_examen);


--
-- TOC entry 3399 (class 2606 OID 18488)
-- Name: examen_resultado pk_examen_resultado; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.examen_resultado
    ADD CONSTRAINT pk_examen_resultado PRIMARY KEY (id_examen_resultado);


--
-- TOC entry 3393 (class 2606 OID 18422)
-- Name: examen_tipo_examen pk_examen_tipo_examen; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.examen_tipo_examen
    ADD CONSTRAINT pk_examen_tipo_examen PRIMARY KEY (id_examen_tipo_examen);


--
-- TOC entry 3366 (class 2606 OID 18236)
-- Name: medio_contacto pk_medio_contacto; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medio_contacto
    ADD CONSTRAINT pk_medio_contacto PRIMARY KEY (id_medio_contacto);


--
-- TOC entry 3397 (class 2606 OID 18460)
-- Name: orden_examen pk_orden_examen; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orden_examen
    ADD CONSTRAINT pk_orden_examen PRIMARY KEY (id_orden_examen);


--
-- TOC entry 3364 (class 2606 OID 18228)
-- Name: persona pk_persona; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.persona
    ADD CONSTRAINT pk_persona PRIMARY KEY (id_persona);


--
-- TOC entry 3373 (class 2606 OID 18276)
-- Name: persona_rol pk_persona_rol; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.persona_rol
    ADD CONSTRAINT pk_persona_rol PRIMARY KEY (id_persona_rol);


--
-- TOC entry 3379 (class 2606 OID 18340)
-- Name: procedimiento pk_procedimiento; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento
    ADD CONSTRAINT pk_procedimiento PRIMARY KEY (id_procedimiento);


--
-- TOC entry 3381 (class 2606 OID 18346)
-- Name: procedimiento_paso pk_procedimiento_paso; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento_paso
    ADD CONSTRAINT pk_procedimiento_paso PRIMARY KEY (id_procedimiento_paso);


--
-- TOC entry 3395 (class 2606 OID 18442)
-- Name: procedimiento_paso_examen pk_procedimiento_paso_examen; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento_paso_examen
    ADD CONSTRAINT pk_procedimiento_paso_examen PRIMARY KEY (id_procedimiento_paso_examen);


--
-- TOC entry 3383 (class 2606 OID 18356)
-- Name: procedimiento_paso_secuencia pk_procedimiento_paso_secuencia; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento_paso_secuencia
    ADD CONSTRAINT pk_procedimiento_paso_secuencia PRIMARY KEY (id_procedimiento_paso_secuencia);


--
-- TOC entry 3370 (class 2606 OID 18270)
-- Name: rol pk_rol; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rol
    ADD CONSTRAINT pk_rol PRIMARY KEY (id_rol);


--
-- TOC entry 3362 (class 2606 OID 18220)
-- Name: tipo_documento pk_tipo_documento; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_documento
    ADD CONSTRAINT pk_tipo_documento PRIMARY KEY (id_tipo_documento);


--
-- TOC entry 3389 (class 2606 OID 18406)
-- Name: tipo_examen pk_tipo_examen; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_examen
    ADD CONSTRAINT pk_tipo_examen PRIMARY KEY (id_tipo_examen);


--
-- TOC entry 3360 (class 2606 OID 18212)
-- Name: tipo_medio_contacto pk_tipo_medio_contacto; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_medio_contacto
    ADD CONSTRAINT pk_tipo_medio_contacto PRIMARY KEY (id_tipo_medio_contacto);


--
-- TOC entry 3371 (class 1259 OID 18315)
-- Name: fki_fk_persona_rol_clinica; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fki_fk_persona_rol_clinica ON public.persona_rol USING btree (id_clinica);


--
-- TOC entry 3407 (class 2606 OID 18329)
-- Name: consulta consulta_persona_rol; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consulta
    ADD CONSTRAINT consulta_persona_rol FOREIGN KEY (id_persona_rol) REFERENCES public.persona_rol(id_persona_rol) ON UPDATE CASCADE ON DELETE RESTRICT NOT VALID;


--
-- TOC entry 3411 (class 2606 OID 18374)
-- Name: consulta_procedimiento fk_consulta_procedimiento_consulta; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consulta_procedimiento
    ADD CONSTRAINT fk_consulta_procedimiento_consulta FOREIGN KEY (id_consulta) REFERENCES public.consulta(id_consulta) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3412 (class 2606 OID 18385)
-- Name: consulta_procedimiento_paso fk_consulta_procedimiento_paso_consulta_procedimiento; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consulta_procedimiento_paso
    ADD CONSTRAINT fk_consulta_procedimiento_paso_consulta_procedimiento FOREIGN KEY (id_consulta_procedimiento) REFERENCES public.consulta_procedimiento(id_consulta_procedimiento) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3413 (class 2606 OID 18390)
-- Name: consulta_procedimiento_paso fk_consulta_procedimiento_paso_persona_rol; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consulta_procedimiento_paso
    ADD CONSTRAINT fk_consulta_procedimiento_paso_persona_rol FOREIGN KEY (id_persona_rol) REFERENCES public.persona_rol(id_persona_rol) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3402 (class 2606 OID 18259)
-- Name: documento fk_documento_persona; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documento
    ADD CONSTRAINT fk_documento_persona FOREIGN KEY (id_persona) REFERENCES public.persona(id_persona) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3403 (class 2606 OID 18254)
-- Name: documento fk_documento_tipo_documento; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documento
    ADD CONSTRAINT fk_documento_tipo_documento FOREIGN KEY (id_tipo_documento) REFERENCES public.tipo_documento(id_tipo_documento) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3419 (class 2606 OID 18489)
-- Name: examen_resultado fk_examen_resultado_orden_examen; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.examen_resultado
    ADD CONSTRAINT fk_examen_resultado_orden_examen FOREIGN KEY (id_orden_examen) REFERENCES public.orden_examen(id_orden_examen) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3414 (class 2606 OID 18423)
-- Name: examen_tipo_examen fk_examen_tipo_examen_examen; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.examen_tipo_examen
    ADD CONSTRAINT fk_examen_tipo_examen_examen FOREIGN KEY (id_examen) REFERENCES public.examen(id_examen) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3415 (class 2606 OID 18428)
-- Name: examen_tipo_examen fk_examen_tipo_examen_tipo_examen; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.examen_tipo_examen
    ADD CONSTRAINT fk_examen_tipo_examen_tipo_examen FOREIGN KEY (id_tipo_examen) REFERENCES public.tipo_examen(id_tipo_examen) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3400 (class 2606 OID 18242)
-- Name: medio_contacto fk_medio_contacto_persona; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medio_contacto
    ADD CONSTRAINT fk_medio_contacto_persona FOREIGN KEY (id_persona) REFERENCES public.persona(id_persona) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3401 (class 2606 OID 18237)
-- Name: medio_contacto fk_medio_contacto_tipo_medio_contacto; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medio_contacto
    ADD CONSTRAINT fk_medio_contacto_tipo_medio_contacto FOREIGN KEY (id_tipo_medio_contacto) REFERENCES public.tipo_medio_contacto(id_tipo_medio_contacto) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3418 (class 2606 OID 18461)
-- Name: orden_examen fk_orden_examen_consulta_procedimiento_paso; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orden_examen
    ADD CONSTRAINT fk_orden_examen_consulta_procedimiento_paso FOREIGN KEY (id_consulta_procedimiento_paso) REFERENCES public.consulta_procedimiento_paso(id_consulta_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3404 (class 2606 OID 18310)
-- Name: persona_rol fk_persona_rol_clinica; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.persona_rol
    ADD CONSTRAINT fk_persona_rol_clinica FOREIGN KEY (id_clinica) REFERENCES public.clinica(id_clinica);


--
-- TOC entry 3405 (class 2606 OID 18277)
-- Name: persona_rol fk_persona_rol_persona; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.persona_rol
    ADD CONSTRAINT fk_persona_rol_persona FOREIGN KEY (id_persona) REFERENCES public.persona(id_persona) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3406 (class 2606 OID 18282)
-- Name: persona_rol fk_persona_rol_rol; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.persona_rol
    ADD CONSTRAINT fk_persona_rol_rol FOREIGN KEY (id_rol) REFERENCES public.rol(id_rol) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3416 (class 2606 OID 18448)
-- Name: procedimiento_paso_examen fk_procedimiento_paso_examen_examen; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento_paso_examen
    ADD CONSTRAINT fk_procedimiento_paso_examen_examen FOREIGN KEY (id_examen) REFERENCES public.examen(id_examen) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3417 (class 2606 OID 18443)
-- Name: procedimiento_paso_examen fk_procedimiento_paso_examen_procedimiento_examen; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento_paso_examen
    ADD CONSTRAINT fk_procedimiento_paso_examen_procedimiento_examen FOREIGN KEY (id_procedimiento_paso) REFERENCES public.procedimiento_paso(id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3408 (class 2606 OID 18347)
-- Name: procedimiento_paso fk_procedimiento_paso_procedimiento; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento_paso
    ADD CONSTRAINT fk_procedimiento_paso_procedimiento FOREIGN KEY (id_procedimiento) REFERENCES public.procedimiento(id_procedimiento) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- TOC entry 3409 (class 2606 OID 18362)
-- Name: procedimiento_paso fk_procedimiento_paso_rol; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento_paso
    ADD CONSTRAINT fk_procedimiento_paso_rol FOREIGN KEY (id_rol) REFERENCES public.rol(id_rol) ON UPDATE CASCADE ON DELETE RESTRICT NOT VALID;


--
-- TOC entry 3410 (class 2606 OID 18357)
-- Name: procedimiento_paso_secuencia fk_procedimiento_paso_secuencia_procedimiento_paso; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procedimiento_paso_secuencia
    ADD CONSTRAINT fk_procedimiento_paso_secuencia_procedimiento_paso FOREIGN KEY (id_procedimiento_paso) REFERENCES public.procedimiento_paso(id_procedimiento_paso) ON UPDATE CASCADE ON DELETE RESTRICT;