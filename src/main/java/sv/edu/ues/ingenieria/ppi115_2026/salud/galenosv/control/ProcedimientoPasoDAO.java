package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPaso;
import java.util.UUID;

@Stateless
public class ProcedimientoPasoDAO extends DefaultDAO<ProcedimientoPaso, UUID> {

    public ProcedimientoPasoDAO() {
        super(ProcedimientoPaso.class);
    }
}
