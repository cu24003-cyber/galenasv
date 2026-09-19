package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenResultado;
import java.util.List;
import java.util.UUID;

@Stateless
public class ExamenResultadoDAO extends DefaultDAO<ExamenResultado, UUID> {

    public ExamenResultadoDAO() {
        super(ExamenResultado.class);
    }

    public List<ExamenResultado> listarPorOrdenExamen(UUID idOrdenExamen) {
        return em.createQuery(
                        "SELECT er FROM ExamenResultado er WHERE er.idOrdenExamen.idOrdenExamen = :idOrdenExamen",
                        ExamenResultado.class)
                .setParameter("idOrdenExamen", idOrdenExamen)
                .getResultList();
    }
}