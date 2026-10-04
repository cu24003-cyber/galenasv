package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPaso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ProcedimientoPasoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

@Stateless
public class ProcedimientoPasoService extends AbstractService<ProcedimientoPaso, UUID> {

    @Inject
    private ProcedimientoPasoRepository procedimientoPasoRepository;

    @Override
    protected RepositoryInterface<ProcedimientoPaso, UUID> getRepository() {
        return procedimientoPasoRepository;
    }

    @Override
    protected UUID obtenerId(ProcedimientoPaso entidad) {
        return entidad.getIdProcedimientoPaso();
    }

    @Override
    public void crear(ProcedimientoPaso entidad) {
        if (entidad.getIdProcedimientoPaso() == null) {
            entidad.setIdProcedimientoPaso(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
