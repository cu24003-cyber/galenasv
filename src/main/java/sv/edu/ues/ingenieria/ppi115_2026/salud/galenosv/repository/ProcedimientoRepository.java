package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Procedimiento;
import java.util.UUID;

@Stateless
public class ProcedimientoRepository extends AbstractRepository<Procedimiento, UUID> {

    public ProcedimientoRepository() {
        super(Procedimiento.class);
    }
}
