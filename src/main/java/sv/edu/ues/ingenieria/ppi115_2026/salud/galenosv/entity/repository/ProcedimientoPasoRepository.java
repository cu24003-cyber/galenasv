package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPaso;

@Stateless
public class ProcedimientoPasoRepository extends AbstractRepository<ProcedimientoPaso, UUID> {

    public ProcedimientoPasoRepository() {
        super(ProcedimientoPaso.class);
    }
}
