package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ProcedimientoPasoExamenService;

@Named("procedimientoPasoExamenModel")
@ViewScoped
public class ProcedimientoPasoExamenModel extends AbstractModel<ProcedimientoPasoExamen, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ProcedimientoPasoExamenService procedimientoPasoExamenService;

    @Override
    protected ProcedimientoPasoExamenService getService() {
        return procedimientoPasoExamenService;
    }

    @Override
    protected ProcedimientoPasoExamen nuevaInstancia() {
        return new ProcedimientoPasoExamen();
    }

    @Override
    protected UUID obtenerId(ProcedimientoPasoExamen entidad) {
        return entidad.getIdProcedimientoPasoExamen();
    }
}
