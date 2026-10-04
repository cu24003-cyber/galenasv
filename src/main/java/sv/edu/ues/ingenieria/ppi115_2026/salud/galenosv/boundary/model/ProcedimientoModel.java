package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Procedimiento;

@Named("procedimientoModel")
@ViewScoped
public class ProcedimientoModel extends AbstractModel<Procedimiento, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ProcedimientoService procedimientoService;

    @Override
    protected ProcedimientoService getService() {
        return procedimientoService;
    }

    @Override
    protected Procedimiento nuevaInstancia() {
        return new Procedimiento();
    }

    @Override
    protected UUID obtenerId(Procedimiento entidad) {
        return entidad.getIdProcedimiento();
    }
}
