package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimiento;
import java.util.UUID;

@Stateless
public class ConsultaProcedimientoRepository extends AbstractRepository<ConsultaProcedimiento, UUID> {

    public ConsultaProcedimientoRepository() {
        super(ConsultaProcedimiento.class);
    }
}
