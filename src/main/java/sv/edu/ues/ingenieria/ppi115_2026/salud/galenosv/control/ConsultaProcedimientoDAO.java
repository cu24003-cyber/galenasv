package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimiento;
import java.util.UUID;

@Stateless
public class ConsultaProcedimientoDAO extends DefaultDAO<ConsultaProcedimiento, UUID> {

    public ConsultaProcedimientoDAO() {
        super(ConsultaProcedimiento.class);
    }
}
