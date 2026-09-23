package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ProcedimientoPasoSecuenciaRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoSecuencia;
import java.util.UUID;


@Stateless
public class ProcedimientoPasoSecuenciaService extends AbstractService<ProcedimientoPasoSecuencia, UUID> {

    @Inject
    private ProcedimientoPasoSecuenciaRepository procedimientoPasoSecuenciaRepository;

    @Override
    protected RepositoryInterface<ProcedimientoPasoSecuencia, UUID> getRepository() {
        return procedimientoPasoSecuenciaRepository;
    }

    @Override
    protected UUID obtenerId(ProcedimientoPasoSecuencia entidad) {
        return entidad.getIdProcedimientoPasoSecuencia();
    }

    @Override
    public void crear(ProcedimientoPasoSecuencia entidad) {
        if (entidad.getIdProcedimientoPasoSecuencia() == null) {
            entidad.setIdProcedimientoPasoSecuencia(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
