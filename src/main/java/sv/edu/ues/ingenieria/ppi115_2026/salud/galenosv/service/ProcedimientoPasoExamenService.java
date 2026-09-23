package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ProcedimientoPasoExamenRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoExamen;
import java.util.UUID;


@Stateless
public class ProcedimientoPasoExamenService extends AbstractService<ProcedimientoPasoExamen, UUID> {

    @Inject
    private ProcedimientoPasoExamenRepository procedimientoPasoExamenRepository;

    @Override
    protected RepositoryInterface<ProcedimientoPasoExamen, UUID> getRepository() {
        return procedimientoPasoExamenRepository;
    }

    @Override
    protected UUID obtenerId(ProcedimientoPasoExamen entidad) {
        return entidad.getIdProcedimientoPasoExamen();
    }

    @Override
    public void crear(ProcedimientoPasoExamen entidad) {
        if (entidad.getIdProcedimientoPasoExamen() == null) {
            entidad.setIdProcedimientoPasoExamen(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
