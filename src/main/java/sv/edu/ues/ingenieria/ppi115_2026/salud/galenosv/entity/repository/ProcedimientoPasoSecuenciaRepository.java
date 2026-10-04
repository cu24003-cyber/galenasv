package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPasoSecuencia;

@Stateless
public class ProcedimientoPasoSecuenciaRepository extends AbstractRepository<ProcedimientoPasoSecuencia, UUID> {

    public ProcedimientoPasoSecuenciaRepository() {
        super(ProcedimientoPasoSecuencia.class);
    }
}
