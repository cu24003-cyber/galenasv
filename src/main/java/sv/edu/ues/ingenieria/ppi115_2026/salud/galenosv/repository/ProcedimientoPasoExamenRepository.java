package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoExamen;
import java.util.UUID;

@Stateless
public class ProcedimientoPasoExamenRepository extends AbstractRepository<ProcedimientoPasoExamen, UUID> {

    public ProcedimientoPasoExamenRepository() {
        super(ProcedimientoPasoExamen.class);
    }
}
