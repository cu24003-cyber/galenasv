package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Procedimiento;
import java.util.UUID;

@Stateless
public class ProcedimientoDAO extends DefaultDAO<Procedimiento, UUID> {

    public ProcedimientoDAO() {
        super(Procedimiento.class);
    }
}
