package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimiento;

@Stateless
public class ConsultaProcedimientoRepository extends AbstractRepository<ConsultaProcedimiento, UUID> {

    public ConsultaProcedimientoRepository() {
        super(ConsultaProcedimiento.class);
    }
}
