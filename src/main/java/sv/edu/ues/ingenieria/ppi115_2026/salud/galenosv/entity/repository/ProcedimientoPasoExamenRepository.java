package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPasoExamen;

@Stateless
public class ProcedimientoPasoExamenRepository extends AbstractRepository<ProcedimientoPasoExamen, UUID> {

    public ProcedimientoPasoExamenRepository() {
        super(ProcedimientoPasoExamen.class);
    }
}
