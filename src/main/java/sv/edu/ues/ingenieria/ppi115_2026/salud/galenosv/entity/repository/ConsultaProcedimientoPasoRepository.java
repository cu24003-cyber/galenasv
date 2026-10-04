package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimientoPaso;

@Stateless
public class ConsultaProcedimientoPasoRepository extends AbstractRepository<ConsultaProcedimientoPaso, UUID> {

    public ConsultaProcedimientoPasoRepository() {
        super(ConsultaProcedimientoPaso.class);
    }
}
