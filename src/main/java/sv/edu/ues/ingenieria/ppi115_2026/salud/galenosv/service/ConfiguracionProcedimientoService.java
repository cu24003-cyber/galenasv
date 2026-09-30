package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
@Stateless
public class ConfiguracionProcedimientoService {
    @PersistenceContext EntityManager em;
    public List<Procedimiento> listar() { return em.createQuery("SELECT p FROM Procedimiento p ORDER BY p.nombre",Procedimiento.class).getResultList(); }
    public List<Rol> roles() { return em.createQuery("SELECT r FROM Rol r WHERE r.activo=true ORDER BY r.nombre",Rol.class).getResultList(); }
    public List<ProcedimientoPaso> pasos(UUID id) { return em.createQuery("SELECT p FROM ProcedimientoPaso p LEFT JOIN FETCH p.idRol WHERE p.idProcedimiento.idProcedimiento=:id ORDER BY p.nombre",ProcedimientoPaso.class).setParameter("id",id).getResultList(); }
    public List<ProcedimientoPasoSecuencia> secuencias(UUID id) { return em.createQuery("SELECT s FROM ProcedimientoPasoSecuencia s JOIN FETCH s.idProcedimientoPaso p JOIN FETCH s.idProcedimientoPasoReferencia WHERE p.idProcedimiento.idProcedimiento=:id",ProcedimientoPasoSecuencia.class).setParameter("id",id).getResultList(); }
    public List<Examen> examenes() { return em.createQuery("SELECT e FROM Examen e WHERE e.activo=true ORDER BY e.nombre",Examen.class).getResultList(); }
    public List<ProcedimientoPasoExamen> asociaciones(UUID id) { return em.createQuery("SELECT e FROM ProcedimientoPasoExamen e JOIN FETCH e.idExamen JOIN FETCH e.idProcedimientoPaso p WHERE p.idProcedimiento.idProcedimiento=:id",ProcedimientoPasoExamen.class).setParameter("id",id).getResultList(); }
    public void asociar(UUID procedimiento,UUID pasoId,UUID examenId) {
        Procedimiento p=em.find(Procedimiento.class,procedimiento,LockModeType.PESSIMISTIC_WRITE);
        ProcedimientoPaso paso=em.find(ProcedimientoPaso.class,pasoId); Examen examen=em.find(Examen.class,examenId);
        if(p==null || paso==null || !p.equals(paso.getIdProcedimiento()) || examen==null || !Boolean.TRUE.equals(examen.getActivo())) throw new ServiceException("Seleccione un paso del procedimiento y un examen activo.");
        Long existentes=em.createQuery("SELECT COUNT(e) FROM ProcedimientoPasoExamen e WHERE e.idProcedimientoPaso=:paso AND e.idExamen=:examen",Long.class).setParameter("paso",paso).setParameter("examen",examen).getSingleResult();
        if(existentes>0) throw new ServiceException("El examen ya está asociado al paso.");
        ProcedimientoPasoExamen e=new ProcedimientoPasoExamen(UUID.randomUUID()); e.setIdProcedimientoPaso(paso); e.setIdExamen(examen); e.setActivo(true); e.setFechaCreacion(AtencionService.ahora()); em.persist(e);
    }
    public Procedimiento guardar(Procedimiento datos) {
        if(datos.getNombre()==null || datos.getNombre().isBlank()) throw new ServiceException("Ingrese el nombre del procedimiento.");
        Procedimiento p=datos.getIdProcedimiento()==null?new Procedimiento(UUID.randomUUID()):em.find(Procedimiento.class,datos.getIdProcedimiento());
        if(p==null) throw new ServiceException("El procedimiento no existe.");
        p.setNombre(datos.getNombre().trim()); p.setObservaciones(datos.getObservaciones()); p.setActivo(datos.getActivo());
        if(datos.getIdProcedimiento()==null) em.persist(p); return p;
    }
    public void paso(UUID id,String nombre,boolean fin,UUID rolId,UUID previoId) {
        Procedimiento p=em.find(Procedimiento.class,id,LockModeType.PESSIMISTIC_WRITE);
        Rol rol=em.find(Rol.class,rolId);
        if(p==null || nombre==null || nombre.isBlank() || rol==null || !Boolean.TRUE.equals(rol.getActivo())) throw new ServiceException("Indique nombre y rol activo para el paso.");
        ProcedimientoPaso previo=previoId==null?null:em.find(ProcedimientoPaso.class,previoId);
        if(previoId!=null && (previo==null || !p.equals(previo.getIdProcedimiento()) || Boolean.TRUE.equals(previo.getIndicaFin()))) throw new ServiceException("El paso anterior debe pertenecer al procedimiento y no indicar fin.");
        ProcedimientoPaso paso=new ProcedimientoPaso(UUID.randomUUID()); paso.setIdProcedimiento(p); paso.setNombre(nombre.trim()); paso.setIndicaFin(fin); paso.setIdRol(rol); em.persist(paso);
        if(previo!=null) { ProcedimientoPasoSecuencia s=new ProcedimientoPasoSecuencia(UUID.randomUUID()); s.setIdProcedimientoPaso(previo); s.setIdProcedimientoPasoReferencia(paso); s.setTipoSecuencia("SIGUIENTE"); em.persist(s); }
    }
}
