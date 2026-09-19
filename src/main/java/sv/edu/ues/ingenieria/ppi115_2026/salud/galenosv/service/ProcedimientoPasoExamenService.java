package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.ProcedimientoPasoExamenDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoExamen;
import java.util.UUID;


@Stateless
public class ProcedimientoPasoExamenService extends AbstractService<ProcedimientoPasoExamen, UUID> {

    @Inject
    private ProcedimientoPasoExamenDAO procedimientoPasoExamenDAO;

    @Override
    protected DefaultDAOInterface<ProcedimientoPasoExamen, UUID> getDao() {
        return procedimientoPasoExamenDAO;
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
