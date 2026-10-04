package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPaso;

@Named("procedimientoPasoModel")
@ViewScoped
public class ProcedimientoPasoModel extends AbstractModel<ProcedimientoPaso, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ProcedimientoPasoService procedimientoPasoService;

    @Override
    protected ProcedimientoPasoService getService() {
        return procedimientoPasoService;
    }

    @Override
    protected ProcedimientoPaso nuevaInstancia() {
        return new ProcedimientoPaso();
    }

    @Override
    protected UUID obtenerId(ProcedimientoPaso entidad) {
        return entidad.getIdProcedimientoPaso();
    }
}
