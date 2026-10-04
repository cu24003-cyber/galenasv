package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Procedimiento;

@Stateless
public class ProcedimientoRepository extends AbstractRepository<Procedimiento, UUID> {

    public ProcedimientoRepository() {
        super(Procedimiento.class);
    }

    public Procedimiento bloquear(UUID id) {
        return em.find(Procedimiento.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public boolean registradoEnConsultas(UUID id) {
        return em.createQuery("SELECT COUNT(c) FROM ConsultaProcedimiento c "
                + "WHERE c.idProcedimiento.idProcedimiento=:id", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }

    public void eliminarConfiguracion(UUID id) {
        // Las dependencias y asociaciones pertenecen al procedimiento; los catálogos se conservan.
        String pasos = "SELECT p FROM ProcedimientoPaso p WHERE p.idProcedimiento.idProcedimiento=:id";
        em.createQuery("DELETE FROM ProcedimientoPasoSecuencia s WHERE s.idProcedimientoPaso IN ("
                + pasos + ") OR s.idProcedimientoPasoReferencia IN (" + pasos + ")")
                .setParameter("id", id).executeUpdate();
        em.createQuery("DELETE FROM ProcedimientoPasoExamen e WHERE e.idProcedimientoPaso IN (" + pasos + ")")
                .setParameter("id", id).executeUpdate();
        em.createQuery("DELETE FROM ProcedimientoPaso p WHERE p.idProcedimiento.idProcedimiento=:id")
                .setParameter("id", id).executeUpdate();
    }
}
