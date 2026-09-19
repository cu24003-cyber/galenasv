package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoExamen;
import java.util.UUID;

@Stateless
public class ProcedimientoPasoExamenDAO extends DefaultDAO<ProcedimientoPasoExamen, UUID> {

    public ProcedimientoPasoExamenDAO() {
        super(ProcedimientoPasoExamen.class);
    }
}
