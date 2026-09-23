package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimientoPaso;
import java.util.UUID;

@Stateless
public class ConsultaProcedimientoPasoRepository extends AbstractRepository<ConsultaProcedimientoPaso, UUID> {

    public ConsultaProcedimientoPasoRepository() {
        super(ConsultaProcedimientoPaso.class);
    }
}
