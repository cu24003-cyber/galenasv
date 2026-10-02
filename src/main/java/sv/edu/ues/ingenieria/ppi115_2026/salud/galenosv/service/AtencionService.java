package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model.AtencionSesion;

/** Operaciones de atención: cada acción se confirma en una sola transacción. */
@Stateless
public class AtencionService {
    @PersistenceContext EntityManager em;
    @Inject AtencionSesion sesion;
    static OffsetDateTime ahora() { return OffsetDateTime.now(ZoneOffset.UTC); }
    private static String texto(String valor, String campo, int maximo) {
        if (valor != null && valor.length() > maximo) throw new ServiceException(campo + " admite hasta " + maximo + " caracteres.");
        return valor == null ? null : valor.trim();
    }
    public List<PersonaRol> pacientes(UUID persona) {
        return pacientesClinica().stream().filter(r -> r.getIdPersona().getIdPersona().equals(persona)).toList();
    }
    public List<PersonaRol> pacientesClinica() {
        PersonaRol medico = medicoActivo();
        return em.createQuery("SELECT r FROM PersonaRol r JOIN FETCH r.idPersona p JOIN FETCH r.idRol rol JOIN FETCH r.idClinica WHERE r.idClinica=:clinica AND LOWER(TRIM(rol.nombre))='paciente' AND rol.activo=true ORDER BY p.apellidos, p.nombres", PersonaRol.class)
                .setParameter("clinica", medico.getIdClinica()).getResultList();
    }
    public List<PersonaRol> responsables(UUID clinica) {
        return em.createQuery("SELECT r FROM PersonaRol r JOIN FETCH r.idPersona p JOIN FETCH r.idRol rol JOIN FETCH r.idClinica WHERE r.idClinica.idClinica=:clinica AND rol.activo=true ORDER BY p.apellidos, p.nombres, rol.nombre", PersonaRol.class)
                .setParameter("clinica", clinica).getResultList();
    }
    private PersonaRol responsable(UUID id, PersonaRol paciente) {
        PersonaRol rol = id == null ? null : em.find(PersonaRol.class, id);
        if (rol == null || rol.getIdRol() == null || !Boolean.TRUE.equals(rol.getIdRol().getActivo())
                || rol.getIdClinica() == null || !rol.getIdClinica().equals(paciente.getIdClinica())) {
            throw new ServiceException("Seleccione un responsable con rol activo de la clínica de la consulta.");
        }
        return rol;
    }
    private PersonaRol medicoActivo() {
        PersonaRol actual = sesion == null ? null : sesion.getRolActivo();
        if (actual == null || actual.getIdPersonaRol() == null) throw new ServiceException("Seleccione su rol médico y clínica.");
        PersonaRol medico = em.find(PersonaRol.class, actual.getIdPersonaRol());
        String nombreRol = medico == null || medico.getIdRol() == null ? "" : medico.getIdRol().getNombre();
        String rolNormalizado = nombreRol == null ? "" : java.text.Normalizer.normalize(nombreRol.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (medico == null || medico.getIdRol() == null || medico.getIdClinica() == null
                || !Boolean.TRUE.equals(medico.getIdRol().getActivo())
                || !Set.of("medico", "medica", "doctor", "doctora").contains(rolNormalizado)) {
            throw new ServiceException("Solo un médico con rol activo puede consultar expedientes clínicos.");
        }
        return medico;
    }
    private void comprobarPropietario(Consulta consulta) {
        PersonaRol medico = medicoActivo();
        if (consulta == null || consulta.getIdMedicoRol() == null
                || !medico.getIdPersonaRol().equals(consulta.getIdMedicoRol().getIdPersonaRol())
                || consulta.getIdPersonaRol() == null || consulta.getIdPersonaRol().getIdClinica() == null
                || !medico.getIdClinica().getIdClinica().equals(consulta.getIdPersonaRol().getIdClinica().getIdClinica())) {
            throw new ServiceException("Esta consulta pertenece a otro médico o clínica.");
        }
    }
    public Consulta abrir(UUID rolId, UUID responsableId) {
        return iniciarConsulta(rolId, responsableId, null, null, true);
    }
    public Consulta crear(UUID pacienteId, String referencia, String observaciones) {
        if (sesion != null && sesion.getConsulta() != null) throw new ServiceException("Cierre la consulta en curso antes de crear otra.");
        return iniciarConsulta(pacienteId, null, referencia, observaciones, false);
    }
    private Consulta iniciarConsulta(UUID rolId, UUID responsableId, String referencia, String observaciones, boolean retomar) {
        PersonaRol medico = medicoActivo();
        if (retomar && !medico.getIdPersonaRol().equals(responsableId)) throw new ServiceException("El médico activo debe abrir su propia consulta.");
        String referenciaValidada = texto(referencia, "La referencia", 255);
        String notasValidadas = texto(observaciones, "Las observaciones", 255);
        PersonaRol rol=rolId == null ? null : em.find(PersonaRol.class,rolId,LockModeType.PESSIMISTIC_WRITE);
        if(rol==null || rol.getIdRol()==null || rol.getIdClinica()==null || rol.getIdRol().getNombre()==null || !"paciente".equalsIgnoreCase(rol.getIdRol().getNombre().trim()) || !Boolean.TRUE.equals(rol.getIdRol().getActivo())) throw new ServiceException("Seleccione el rol del paciente.");
        responsable(medico.getIdPersonaRol(), rol);
        List<Consulta> abiertas=em.createQuery("SELECT c FROM Consulta c WHERE c.idPersonaRol=:rol AND c.fechaFin IS NULL",Consulta.class).setParameter("rol",rol).getResultList();
        if(!abiertas.isEmpty()) {
            comprobarPropietario(abiertas.get(0));
            if (!retomar) throw new ServiceException("El paciente ya tiene una consulta en curso. Retómela desde su historial.");
            return cargar(abiertas.get(0).getIdConsulta());
        }
        Consulta c=new Consulta(UUID.randomUUID()); c.setIdPersonaRol(rol); c.setIdMedicoRol(medico); c.setFechaInicio(ahora());
        c.setReferenciaExterna(referenciaValidada); c.setObservaciones(notasValidadas);
        em.persist(c); em.flush(); return cargar(c.getIdConsulta());
    }
    public Consulta cargar(UUID id) {
        Consulta c = em.createQuery("SELECT c FROM Consulta c JOIN FETCH c.idPersonaRol r JOIN FETCH r.idPersona JOIN FETCH r.idClinica LEFT JOIN FETCH c.idMedicoRol m LEFT JOIN FETCH m.idPersona WHERE c.idConsulta=:id",Consulta.class).setParameter("id",id).getSingleResult();
        comprobarPropietario(c);
        return c;
    }
    public List<Consulta> historial() { return historial(null, null); }
    public List<Consulta> historial(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta))
            throw ServiceException.localizada("consulta.rangoInvalido", "La fecha Desde debe ser anterior o igual a Hasta.");
        PersonaRol medico = medicoActivo();
        ZoneId zona = ZoneId.of("America/El_Salvador");
        OffsetDateTime inicio = desde == null ? null : desde.atStartOfDay(zona).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime finExclusivo = hasta == null ? null : hasta.plusDays(1).atStartOfDay(zona).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        String jpql = "SELECT c FROM Consulta c JOIN FETCH c.idPersonaRol paciente JOIN FETCH paciente.idPersona JOIN FETCH paciente.idClinica WHERE c.idMedicoRol=:medico AND paciente.idClinica=:clinica"
                + (inicio == null ? "" : " AND c.fechaInicio >= :desde")
                + (finExclusivo == null ? "" : " AND c.fechaInicio < :hasta") + " ORDER BY c.fechaInicio DESC";
        TypedQuery<Consulta> consulta = em.createQuery(jpql, Consulta.class)
                .setParameter("medico", medico).setParameter("clinica", medico.getIdClinica());
        if (inicio != null) consulta.setParameter("desde", inicio);
        if (finExclusivo != null) consulta.setParameter("hasta", finExclusivo);
        return consulta.getResultList();
    }
    private Consulta abierta(UUID id) {
        Consulta c=em.find(Consulta.class,id,LockModeType.PESSIMISTIC_WRITE);
        comprobarPropietario(c);
        if(c.getFechaFin()!=null) throw new ServiceException("La consulta ya está cerrada."); return c;
    }
    public void guardar(UUID id,String referencia,String observaciones) {
        Consulta c=abierta(id); c.setReferenciaExterna(texto(referencia,"La referencia",255)); c.setObservaciones(texto(observaciones,"Las observaciones",255));
    }
    public List<Procedimiento> procedimientos() { return em.createQuery("SELECT p FROM Procedimiento p WHERE p.activo=true ORDER BY p.nombre",Procedimiento.class).getResultList(); }
    public List<ConsultaProcedimiento> realizados(UUID id) {
        cargar(id);
        return em.createQuery("SELECT p FROM ConsultaProcedimiento p JOIN FETCH p.idProcedimiento WHERE p.idConsulta.idConsulta=:id ORDER BY p.fechaInicio",ConsultaProcedimiento.class).setParameter("id",id).getResultList();
    }
    public List<ConsultaProcedimientoPaso> pasos(UUID id) {
        cargar(id);
        return em.createQuery("SELECT p FROM ConsultaProcedimientoPaso p JOIN FETCH p.idConsultaProcedimiento cp JOIN FETCH cp.idProcedimiento LEFT JOIN FETCH p.idProcedimientoPaso definicion LEFT JOIN FETCH definicion.idRol LEFT JOIN FETCH p.idPersonaRol responsable LEFT JOIN FETCH responsable.idPersona LEFT JOIN FETCH responsable.idRol WHERE cp.idConsulta.idConsulta=:id ORDER BY p.fechaInicio, p.idConsultaProcedimientoPaso",ConsultaProcedimientoPaso.class).setParameter("id",id).getResultList();
    }
    public void agregarProcedimiento(UUID consulta,UUID procedimiento,String notas,UUID responsableId) {
        Consulta c=abierta(consulta); Procedimiento p=em.find(Procedimiento.class,procedimiento);
        if(p==null || !Boolean.TRUE.equals(p.getActivo())) throw new ServiceException("Seleccione un procedimiento activo.");
        List<ProcedimientoPaso> catalogo=em.createQuery("SELECT p FROM ProcedimientoPaso p WHERE p.idProcedimiento=:p",ProcedimientoPaso.class).setParameter("p",p).getResultList();
        if(catalogo.isEmpty()) throw new ServiceException("Configure los pasos del procedimiento antes de utilizarlo.");
        List<ProcedimientoPasoSecuencia> secuencias=em.createQuery("SELECT s FROM ProcedimientoPasoSecuencia s WHERE s.idProcedimientoPaso.idProcedimiento=:p",ProcedimientoPasoSecuencia.class).setParameter("p",p).getResultList();
        Set<UUID> destinos = new HashSet<>();
        for (ProcedimientoPasoSecuencia s : secuencias) destinos.add(s.getIdProcedimientoPasoReferencia().getIdProcedimientoPaso());
        List<ProcedimientoPaso> iniciales = catalogo.stream().filter(paso -> !destinos.contains(paso.getIdProcedimientoPaso())).toList();
        if (iniciales.isEmpty()) throw ServiceException.localizada("consulta.procedimientoSinInicial", "El procedimiento no tiene un paso inicial.");
        Map<UUID, PersonaRol> encargados = new HashMap<>();
        for (ProcedimientoPaso paso : iniciales) encargados.put(paso.getIdProcedimientoPaso(), responsableAutomatico(c, paso));
        ConsultaProcedimiento cp=new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c); cp.setIdProcedimiento(p); cp.setFechaInicio(c.getFechaInicio()); cp.setObservaciones(texto(notas,"Las observaciones del procedimiento",255)); em.persist(cp);
        for (ProcedimientoPaso paso : iniciales) crearPaso(cp, paso, encargados.get(paso.getIdProcedimientoPaso()), c.getFechaInicio());
    }
    private PersonaRol responsableAutomatico(Consulta consulta, ProcedimientoPaso paso) {
        if (paso.getIdRol() == null || !Boolean.TRUE.equals(paso.getIdRol().getActivo()))
            throw ServiceException.localizada("consulta.pasoRolInactivo", "El paso " + paso.getNombre() + " no tiene un rol activo.", paso.getNombre());
        List<PersonaRol> disponibles = em.createQuery("SELECT r FROM PersonaRol r WHERE r.idClinica=:clinica AND r.idRol=:rol AND r.idRol.activo=true ORDER BY r.fechaCreacion, r.idPersonaRol", PersonaRol.class)
                .setParameter("clinica", consulta.getIdPersonaRol().getIdClinica()).setParameter("rol", paso.getIdRol())
                .setMaxResults(1).getResultList();
        if (disponibles.isEmpty()) throw ServiceException.localizada("consulta.sinResponsableAutomatico",
                "No hay una persona activa con rol " + paso.getIdRol().getNombre() + " en la clínica de la consulta para el paso " + paso.getNombre() + ".",
                paso.getIdRol().getNombre(), paso.getNombre());
        return disponibles.get(0);
    }
    private void crearPaso(ConsultaProcedimiento cp, ProcedimientoPaso definicion, PersonaRol encargado, OffsetDateTime inicio) {
        ConsultaProcedimientoPaso ejecucion = new ConsultaProcedimientoPaso(UUID.randomUUID());
        ejecucion.setIdConsultaProcedimiento(cp); ejecucion.setIdProcedimientoPaso(definicion);
        ejecucion.setIdPersonaRol(encargado); ejecucion.setFechaInicio(inicio); ejecucion.setEstado("PENDIENTE"); em.persist(ejecucion);
    }
    public void asignarResponsable(UUID consulta, UUID pasoId, UUID responsableId) {
        Consulta c = abierta(consulta);
        ConsultaProcedimientoPaso paso = pasoPropio(consulta, pasoId);
        if (paso.getFechaFin() != null) throw new ServiceException("El paso ya está completado.");
        paso.setIdPersonaRol(responsable(responsableId, c.getIdPersonaRol()));
    }
    public void completar(UUID consulta,UUID pasoId) {
        abierta(consulta); ConsultaProcedimientoPaso paso=pasoPropio(consulta,pasoId);
        if(paso.getFechaFin()!=null) return;
        List<ProcedimientoPasoSecuencia> previos=em.createQuery("SELECT s FROM ProcedimientoPasoSecuencia s WHERE s.idProcedimientoPasoReferencia=:p",ProcedimientoPasoSecuencia.class).setParameter("p",paso.getIdProcedimientoPaso()).getResultList();
        for(ProcedimientoPasoSecuencia s:previos) {
            Long pendientes=em.createQuery("SELECT COUNT(p) FROM ConsultaProcedimientoPaso p WHERE p.idConsultaProcedimiento=:cp AND p.idProcedimientoPaso=:previo AND p.fechaFin IS NULL",Long.class).setParameter("cp",paso.getIdConsultaProcedimiento()).setParameter("previo",s.getIdProcedimientoPaso()).getSingleResult();
            if(pendientes>0) throw new ServiceException("Complete primero los pasos anteriores.");
        }
        paso.setEstado("COMPLETADO"); paso.setFechaFin(ahora());
        List<ProcedimientoPasoSecuencia> siguientes=em.createQuery("SELECT s FROM ProcedimientoPasoSecuencia s WHERE s.idProcedimientoPaso=:p",ProcedimientoPasoSecuencia.class)
                .setParameter("p",paso.getIdProcedimientoPaso()).getResultList();
        for (ProcedimientoPasoSecuencia siguiente : siguientes) {
            ProcedimientoPaso destino = siguiente.getIdProcedimientoPasoReferencia();
            List<ConsultaProcedimientoPaso> existentes=em.createQuery("SELECT p FROM ConsultaProcedimientoPaso p WHERE p.idConsultaProcedimiento=:cp AND p.idProcedimientoPaso=:destino",ConsultaProcedimientoPaso.class)
                    .setParameter("cp",paso.getIdConsultaProcedimiento()).setParameter("destino",destino).getResultList();
            if (!existentes.isEmpty()) continue;
            List<ProcedimientoPasoSecuencia> requisitos=em.createQuery("SELECT s FROM ProcedimientoPasoSecuencia s WHERE s.idProcedimientoPasoReferencia=:destino",ProcedimientoPasoSecuencia.class)
                    .setParameter("destino",destino).getResultList();
            boolean completos = true;
            for (ProcedimientoPasoSecuencia requisito : requisitos) {
                Long total=em.createQuery("SELECT COUNT(p) FROM ConsultaProcedimientoPaso p WHERE p.idConsultaProcedimiento=:cp AND p.idProcedimientoPaso=:previo AND p.fechaFin IS NOT NULL",Long.class)
                        .setParameter("cp",paso.getIdConsultaProcedimiento()).setParameter("previo",requisito.getIdProcedimientoPaso()).getSingleResult();
                if (total == 0) { completos = false; break; }
            }
            if (completos) crearPaso(paso.getIdConsultaProcedimiento(), destino,
                    responsableAutomatico(paso.getIdConsultaProcedimiento().getIdConsulta(), destino), paso.getFechaFin());
        }
    }
    private ConsultaProcedimientoPaso pasoPropio(UUID consulta,UUID id) {
        ConsultaProcedimientoPaso p=id==null?null:em.find(ConsultaProcedimientoPaso.class,id);
        if(p==null || !consulta.equals(p.getIdConsultaProcedimiento().getIdConsulta().getIdConsulta())) throw new ServiceException("Seleccione un paso de esta consulta."); return p;
    }
    public List<TipoExamen> tipos(String texto) { return em.createQuery("SELECT t FROM TipoExamen t WHERE t.activo=true AND LOWER(t.nombre) LIKE :q ORDER BY t.nombre",TipoExamen.class).setParameter("q","%"+(texto == null ? "" : texto.toLowerCase(Locale.ROOT))+"%").setMaxResults(30).getResultList(); }
    public void examen(UUID consulta,UUID pasoId,String nombre,String notas,UUID tipoId) {
        abierta(consulta); ConsultaProcedimientoPaso paso=pasoPropio(consulta,pasoId);
        TipoExamen tipo=tipoId==null?null:em.find(TipoExamen.class,tipoId);
        if(nombre==null || nombre.isBlank() || nombre.trim().length()>155 || tipo==null || !Boolean.TRUE.equals(tipo.getActivo())) throw new ServiceException("Indique un nombre de hasta 155 caracteres y un tipo de examen activo.");
        if(paso.getFechaFin()!=null) throw new ServiceException("No se pueden registrar exámenes en un paso completado.");
        if(paso.getIdProcedimientoPaso()==null) throw new ServiceException("El paso no tiene definición de catálogo.");
        Examen e=new Examen(UUID.randomUUID()); e.setNombre(nombre.trim()); e.setActivo(true); e.setObservaciones(texto(notas,"Las observaciones del examen",255)); em.persist(e);
        ExamenTipoExamen et=new ExamenTipoExamen(UUID.randomUUID()); et.setIdExamen(e); et.setIdTipoExamen(tipo); et.setFechaCreacion(ahora()); em.persist(et);
        ProcedimientoPasoExamen pe=new ProcedimientoPasoExamen(UUID.randomUUID()); pe.setIdExamen(e); pe.setIdProcedimientoPaso(paso.getIdProcedimientoPaso()); pe.setActivo(true); pe.setFechaCreacion(ahora()); em.persist(pe);
    }
    public List<ProcedimientoPasoExamen> examenes(UUID consulta) {
        cargar(consulta);
        return em.createQuery("SELECT DISTINCT e FROM ProcedimientoPasoExamen e JOIN FETCH e.idExamen JOIN FETCH e.idProcedimientoPaso WHERE e.idProcedimientoPaso IN (SELECT p.idProcedimientoPaso FROM ConsultaProcedimientoPaso p WHERE p.idConsultaProcedimiento.idConsulta.idConsulta=:id)",ProcedimientoPasoExamen.class).setParameter("id",consulta).getResultList();
    }
    public void ordenar(UUID consulta,UUID paso,String indicaciones) {
        abierta(consulta); ConsultaProcedimientoPaso p=pasoPropio(consulta,paso);
        if(indicaciones==null || indicaciones.isBlank() || indicaciones.trim().length()>255) throw new ServiceException("Ingrese indicaciones de hasta 255 caracteres.");
        if(p.getFechaFin()!=null) throw new ServiceException("No se pueden crear órdenes en un paso completado.");
        OrdenExamen o=new OrdenExamen(UUID.randomUUID()); o.setIdConsultaProcedimientoPaso(p); o.setIndicaciones(indicaciones.trim()); o.setFechaCreacion(ahora()); em.persist(o);
    }
    public List<OrdenExamen> ordenes(UUID consulta) { cargar(consulta); return em.createQuery("SELECT o FROM OrdenExamen o JOIN FETCH o.idConsultaProcedimientoPaso p LEFT JOIN FETCH p.idProcedimientoPaso WHERE p.idConsultaProcedimiento.idConsulta.idConsulta=:id ORDER BY o.fechaCreacion",OrdenExamen.class).setParameter("id",consulta).getResultList(); }
    public List<ExamenResultado> resultados(UUID consulta) {
        cargar(consulta);
        return em.createQuery("SELECT r FROM ExamenResultado r JOIN FETCH r.idOrdenExamen o JOIN FETCH o.idConsultaProcedimientoPaso p WHERE p.idConsultaProcedimiento.idConsulta.idConsulta=:id ORDER BY r.fechaCreacion DESC", ExamenResultado.class)
                .setParameter("id", consulta).getResultList();
    }
    public void registrarResultado(UUID consulta, UUID ordenId, String resultado, String interpretacion) {
        cargar(consulta);
        OrdenExamen orden = ordenId == null ? null : em.find(OrdenExamen.class, ordenId);
        if (orden == null || orden.getIdConsultaProcedimientoPaso() == null
                || !consulta.equals(orden.getIdConsultaProcedimientoPaso().getIdConsultaProcedimiento().getIdConsulta().getIdConsulta())) {
            throw new ServiceException("Seleccione una orden de esta consulta.");
        }
        if (resultado == null || resultado.isBlank() || resultado.trim().length() > 255
                || (interpretacion != null && interpretacion.length() > 255)) {
            throw new ServiceException("Ingrese un resultado de hasta 255 caracteres; la interpretación también admite 255.");
        }
        ExamenResultado registro = new ExamenResultado(UUID.randomUUID());
        registro.setIdOrdenExamen(orden);
        registro.setResultado(resultado.trim());
        registro.setInterpretacion(interpretacion == null ? null : interpretacion.trim());
        em.persist(registro);
    }
    public void cerrar(UUID id,String referencia,String notas) {
        Consulta c=abierta(id);
        List<ConsultaProcedimientoPaso> pasos=pasos(id);
        if(pasos.stream().anyMatch(p->p.getFechaFin()==null)) throw new ServiceException("Complete todos los pasos antes de cerrar la consulta.");
        for (ConsultaProcedimiento procedimiento : realizados(id)) {
            boolean termino = pasos.stream().anyMatch(p -> p.getIdConsultaProcedimiento().getIdConsultaProcedimiento().equals(procedimiento.getIdConsultaProcedimiento())
                    && p.getIdProcedimientoPaso() != null && Boolean.TRUE.equals(p.getIdProcedimientoPaso().getIndicaFin()) && p.getFechaFin() != null);
            if (!termino) throw ServiceException.localizada("consulta.procedimientoSinFinal",
                    "El procedimiento " + procedimiento.getIdProcedimiento().getNombre() + " no ha llegado a un paso final.",
                    procedimiento.getIdProcedimiento().getNombre());
        }
        String referenciaValidada = texto(referencia,"La referencia",255);
        String notasValidadas = texto(notas,"Las observaciones",255);
        OffsetDateTime fin=ahora(); c.setReferenciaExterna(referenciaValidada); c.setObservaciones(notasValidadas); c.setFechaFin(fin);
        for(ConsultaProcedimiento p:realizados(id)) p.setFechaFin(fin);
    }
}
