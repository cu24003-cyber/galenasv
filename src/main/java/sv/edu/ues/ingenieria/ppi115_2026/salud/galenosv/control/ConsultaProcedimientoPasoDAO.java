package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimientoPaso;
import java.util.UUID;

@Stateless
public class ConsultaProcedimientoPasoDAO extends DefaultDAO<ConsultaProcedimientoPaso, UUID> {

    public ConsultaProcedimientoPasoDAO() {
        super(ConsultaProcedimientoPaso.class);
    }
}
