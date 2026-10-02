ALTER TABLE IF EXISTS public.consulta_procedimiento_paso
    ADD COLUMN id_procedimiento_paso uuid;

ALTER TABLE IF EXISTS public.consulta_procedimiento_paso
    ADD COLUMN valor text;

ALTER TABLE IF EXISTS public.consulta_procedimiento_paso
    ADD CONSTRAINT fk_consulta_procedimiento_paso_consulta_paso FOREIGN KEY (id_procedimiento_paso)
    REFERENCES public.procedimiento_paso (id_procedimiento_paso) MATCH SIMPLE
    ON UPDATE CASCADE
    ON DELETE RESTRICT
    NOT VALID;
