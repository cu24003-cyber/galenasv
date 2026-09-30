package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;

/** Operaciones de atención: cada acción se confirma en una sola transacción. */
@Stateless
public class AtencionService {
    @PersistenceContext EntityManager em;
    static OffsetDateTime ahora() { return OffsetDateTime.now(ZoneOffset.UTC); }
    public List<PersonaRol> pacientes(UUID persona) {
        return em.createQuery("SELECT r FROM PersonaRol r JOIN FETCH r.idPersona JOIN FETCH r.idRol rol JOIN FETCH r.idClinica WHERE r.idPersona.idPersona=:id AND LOWER(TRIM(rol.nombre))='paciente' AND rol.activo=true", PersonaRol.class).setParameter("id", persona).getResultList();
    }
    public Consulta abrir(UUID rolId) {
        PersonaRol rol=em.find(PersonaRol.class,rolId,LockModeType.PESSIMISTIC_WRITE);
        if(rol==null || !"paciente".equalsIgnoreCase(rol.getIdRol().getNombre().trim()) || !Boolean.TRUE.equals(rol.getIdRol().getActivo())) throw new ServiceException("Seleccione el rol del paciente.");
        List<Consulta> abiertas=em.createQuery("SELECT c FROM Consulta c WHERE c.idPersonaRol=:rol AND c.fechaFin IS NULL",Consulta.class).setParameter("rol",rol).getResultList();
        if(!abiertas.isEmpty()) return cargar(abiertas.get(0).getIdConsulta());
        Consulta c=new Consulta(UUID.randomUUID()); c.setIdPersonaRol(rol); c.setFechaInicio(ahora()); em.persist(c); em.flush(); return cargar(c.getIdConsulta());
    }
    public Consulta cargar(UUID id) {
        return em.createQuery("SELECT c FROM Consulta c JOIN FETCH c.idPersonaRol r JOIN FETCH r.idPersona JOIN FETCH r.idClinica WHERE c.idConsulta=:id",Consulta.class).setParameter("id",id).getSingleResult();
    }
    private Consulta abierta(UUID id) {
        Consulta c=em.find(Consulta.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(c==null || c.getFechaFin()!=null) throw new ServiceException("La consulta ya está cerrada."); return c;
    }
    public void guardar(UUID id,String referencia,String observaciones) {
        Consulta c=abierta(id); c.setReferenciaExterna(referencia); c.setObservaciones(observaciones);
    }
    public List<Procedimiento> procedimientos() { return em.createQuery("SELECT p FROM Procedimiento p WHERE p.activo=true ORDER BY p.nombre",Procedimiento.class).getResultList(); }
    public List<ConsultaProcedimiento> realizados(UUID id) {
        return em.createQuery("SELECT p FROM ConsultaProcedimiento p JOIN FETCH p.idProcedimiento WHERE p.idConsulta.idConsulta=:id ORDER BY p.fechaInicio",ConsultaProcedimiento.class).setParameter("id",id).getResultList();
    }
    public List<ConsultaProcedimientoPaso> pasos(UUID id) {
        return em.createQuery("SELECT p FROM ConsultaProcedimientoPaso p JOIN FETCH p.idConsultaProcedimiento cp JOIN FETCH cp.idProcedimiento LEFT JOIN FETCH p.idProcedimientoPaso WHERE cp.idConsulta.idConsulta=:id ORDER BY p.fechaInicio, p.idConsultaProcedimientoPaso",ConsultaProcedimientoPaso.class).setParameter("id",id).getResultList();
    }
    public void agregarProcedimiento(UUID consulta,UUID procedimiento,String notas) {
        Consulta c=abierta(consulta); Procedimiento p=em.find(Procedimiento.class,procedimiento);
        if(p==null || !Boolean.TRUE.equals(p.getActivo())) throw new ServiceException("Seleccione un procedimiento activo.");
        List<ProcedimientoPaso> catalogo=em.createQuery("SELECT p FROM ProcedimientoPaso p WHERE p.idProcedimiento=:p",ProcedimientoPaso.class).setParameter("p",p).getResultList();
        if(catalogo.isEmpty()) throw new ServiceException("Configure los pasos del procedimiento antes de utilizarlo.");
        ConsultaProcedimiento cp=new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c); cp.setIdProcedimiento(p); cp.setFechaInicio(c.getFechaInicio()); cp.setObservaciones(notas); em.persist(cp);
        for(ProcedimientoPaso paso:catalogo) { ConsultaProcedimientoPaso ejecucion=new ConsultaProcedimientoPaso(UUID.randomUUID()); ejecucion.setIdConsultaProcedimiento(cp); ejecucion.setIdProcedimientoPaso(paso); ejecucion.setIdPersonaRol(c.getIdPersonaRol()); ejecucion.setFechaInicio(c.getFechaInicio()); ejecucion.setEstado("PENDIENTE"); em.persist(ejecucion); }
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
    }
    private ConsultaProcedimientoPaso pasoPropio(UUID consulta,UUID id) {
        ConsultaProcedimientoPaso p=id==null?null:em.find(ConsultaProcedimientoPaso.class,id);
        if(p==null || !consulta.equals(p.getIdConsultaProcedimiento().getIdConsulta().getIdConsulta())) throw new ServiceException("Seleccione un paso de esta consulta."); return p;
    }
    public List<TipoExamen> tipos(String texto) { return em.createQuery("SELECT t FROM TipoExamen t WHERE t.activo=true AND LOWER(t.nombre) LIKE :q ORDER BY t.nombre",TipoExamen.class).setParameter("q","%"+texto.toLowerCase(Locale.ROOT)+"%").setMaxResults(30).getResultList(); }
    public void examen(UUID consulta,UUID pasoId,String nombre,String notas,UUID tipoId) {
        abierta(consulta); ConsultaProcedimientoPaso paso=pasoPropio(consulta,pasoId);
        TipoExamen tipo=tipoId==null?null:em.find(TipoExamen.class,tipoId);
        if(nombre==null || nombre.isBlank() || tipo==null || !Boolean.TRUE.equals(tipo.getActivo())) throw new ServiceException("Indique nombre y tipo de examen activo.");
        if(paso.getIdProcedimientoPaso()==null) throw new ServiceException("El paso no tiene definición de catálogo.");
        Examen e=new Examen(UUID.randomUUID()); e.setNombre(nombre.trim()); e.setActivo(true); e.setObservaciones(notas); em.persist(e);
        ExamenTipoExamen et=new ExamenTipoExamen(UUID.randomUUID()); et.setIdExamen(e); et.setIdTipoExamen(tipo); et.setFechaCreacion(ahora()); em.persist(et);
        ProcedimientoPasoExamen pe=new ProcedimientoPasoExamen(UUID.randomUUID()); pe.setIdExamen(e); pe.setIdProcedimientoPaso(paso.getIdProcedimientoPaso()); pe.setActivo(true); pe.setFechaCreacion(ahora()); em.persist(pe);
    }
    public List<ProcedimientoPasoExamen> examenes(UUID consulta) {
        return em.createQuery("SELECT DISTINCT e FROM ProcedimientoPasoExamen e JOIN FETCH e.idExamen JOIN FETCH e.idProcedimientoPaso WHERE e.idProcedimientoPaso IN (SELECT p.idProcedimientoPaso FROM ConsultaProcedimientoPaso p WHERE p.idConsultaProcedimiento.idConsulta.idConsulta=:id)",ProcedimientoPasoExamen.class).setParameter("id",consulta).getResultList();
    }
    public void ordenar(UUID consulta,UUID paso,String indicaciones) {
        abierta(consulta); ConsultaProcedimientoPaso p=pasoPropio(consulta,paso);
        if(indicaciones==null || indicaciones.isBlank()) throw new ServiceException("Ingrese las indicaciones de la orden.");
        OrdenExamen o=new OrdenExamen(UUID.randomUUID()); o.setIdConsultaProcedimientoPaso(p); o.setIndicaciones(indicaciones); o.setFechaCreacion(ahora()); em.persist(o);
    }
    public List<OrdenExamen> ordenes(UUID consulta) { return em.createQuery("SELECT o FROM OrdenExamen o JOIN FETCH o.idConsultaProcedimientoPaso p LEFT JOIN FETCH p.idProcedimientoPaso WHERE p.idConsultaProcedimiento.idConsulta.idConsulta=:id ORDER BY o.fechaCreacion",OrdenExamen.class).setParameter("id",consulta).getResultList(); }
    public void cerrar(UUID id,String referencia,String notas) {
        Consulta c=abierta(id);
        List<ConsultaProcedimientoPaso> pasos=pasos(id);
        if(pasos.stream().anyMatch(p->p.getFechaFin()==null)) throw new ServiceException("Complete todos los pasos antes de cerrar la consulta.");
        OffsetDateTime fin=ahora(); c.setReferenciaExterna(referencia); c.setObservaciones(notas); c.setFechaFin(fin);
        for(ConsultaProcedimiento p:realizados(id)) p.setFechaFin(fin);
    }
}
