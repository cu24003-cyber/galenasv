package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPaso;
import java.util.UUID;

@Stateless
public class ProcedimientoPasoRepository extends AbstractRepository<ProcedimientoPaso, UUID> {

    public ProcedimientoPasoRepository() {
        super(ProcedimientoPaso.class);
    }
}
