package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ExamenRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen;
import java.util.*;
import jakarta.persistence.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenTipoExamen;


@Stateless
public class ExamenService extends AbstractService<Examen, UUID> {

    @PersistenceContext private EntityManager em;

    @Inject
    private ExamenRepository examenRepository;

    @Override
    protected RepositoryInterface<Examen, UUID> getRepository() {
        return examenRepository;
    }

    @Override
    protected UUID obtenerId(Examen entidad) {
        return entidad.getIdExamen();
    }

    @Override
    public void crear(Examen entidad) {
        if (entidad.getIdExamen() == null) {
            entidad.setIdExamen(UUID.randomUUID());
        }
        super.crear(entidad);
    }

    public List<TipoExamen> tipos() {
        return em.createQuery("SELECT t FROM TipoExamen t ORDER BY t.nombre", TipoExamen.class).getResultList();
    }

    public List<UUID> tiposAsignados(UUID examen) {
        return em.createQuery("SELECT e.idTipoExamen.idTipoExamen FROM ExamenTipoExamen e WHERE e.idExamen.idExamen=:id", UUID.class)
                .setParameter("id", examen).getResultList();
    }

    /** El examen y sus clasificaciones se guardan en una sola transacción. */
    public void guardarConTipos(Examen datos, List<UUID> tiposIds) {
        if (datos == null || datos.getNombre() == null || datos.getNombre().isBlank() || datos.getNombre().trim().length() > 155) {
            throw new ServiceException("Ingrese un nombre de examen de hasta 155 caracteres.");
        }
        Set<UUID> ids = new LinkedHashSet<>(tiposIds == null ? List.of() : tiposIds);
        if (ids.isEmpty()) throw new ServiceException("Seleccione al menos un tipo de examen.");
        boolean nuevo = datos.getIdExamen() == null;
        Examen examen = nuevo ? new Examen(UUID.randomUUID()) : em.find(Examen.class, datos.getIdExamen(), LockModeType.PESSIMISTIC_WRITE);
        if (examen == null) throw new ServiceException("El examen ya no existe.");
        List<ExamenTipoExamen> anteriores = nuevo ? List.of() : em.createQuery(
                "SELECT e FROM ExamenTipoExamen e JOIN FETCH e.idTipoExamen WHERE e.idExamen=:examen", ExamenTipoExamen.class)
                .setParameter("examen", examen).getResultList();
        List<TipoExamen> tipos = new ArrayList<>();
        for (UUID id : ids) {
            TipoExamen tipo = id == null ? null : em.find(TipoExamen.class, id);
            boolean existente = anteriores.stream().anyMatch(e -> e.getIdTipoExamen().getIdTipoExamen().equals(id));
            if (tipo == null || (!Boolean.TRUE.equals(tipo.getActivo()) && !existente)) {
                throw new ServiceException("Seleccione tipos de examen activos.");
            }
            tipos.add(tipo);
        }
        examen.setNombre(datos.getNombre().trim());
        examen.setActivo(Boolean.TRUE.equals(datos.getActivo()));
        examen.setObservaciones(datos.getObservaciones());
        if (nuevo) em.persist(examen);
        for (ExamenTipoExamen anterior : anteriores) {
            if (!ids.contains(anterior.getIdTipoExamen().getIdTipoExamen())) em.remove(anterior);
        }
        for (TipoExamen tipo : tipos) {
            if (anteriores.stream().noneMatch(e -> e.getIdTipoExamen().equals(tipo))) {
                ExamenTipoExamen clasificacion = new ExamenTipoExamen(UUID.randomUUID());
                clasificacion.setIdExamen(examen);
                clasificacion.setIdTipoExamen(tipo);
                clasificacion.setFechaCreacion(AtencionService.ahora());
                em.persist(clasificacion);
            }
        }
        em.flush();
    }

    public void eliminarConTipos(UUID id) {
        Examen examen = em.find(Examen.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (examen == null) throw new ServiceException("El examen ya no existe.");
        Long usos = em.createQuery("SELECT COUNT(e) FROM ProcedimientoPasoExamen e WHERE e.idExamen=:examen", Long.class)
                .setParameter("examen", examen).getSingleResult();
        if (usos > 0) throw new ServiceException("El examen está asociado a un procedimiento. Puede desactivarlo.");
        em.createQuery("DELETE FROM ExamenTipoExamen e WHERE e.idExamen=:examen").setParameter("examen", examen).executeUpdate();
        em.remove(examen);
        em.flush();
    }

}
