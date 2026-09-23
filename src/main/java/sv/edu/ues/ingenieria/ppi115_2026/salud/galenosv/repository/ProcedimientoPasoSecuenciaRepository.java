package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoSecuencia;
import java.util.UUID;

@Stateless
public class ProcedimientoPasoSecuenciaRepository extends AbstractRepository<ProcedimientoPasoSecuencia, UUID> {

    public ProcedimientoPasoSecuenciaRepository() {
        super(ProcedimientoPasoSecuencia.class);
    }
}
