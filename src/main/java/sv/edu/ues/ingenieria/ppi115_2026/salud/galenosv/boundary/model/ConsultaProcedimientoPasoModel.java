package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimientoPaso;

@Named("consultaProcedimientoPasoModel")
@ViewScoped
public class ConsultaProcedimientoPasoModel extends AbstractModel<ConsultaProcedimientoPaso, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ConsultaProcedimientoPasoService consultaProcedimientoPasoService;

    @Override
    protected ConsultaProcedimientoPasoService getService() {
        return consultaProcedimientoPasoService;
    }

    @Override
    protected ConsultaProcedimientoPaso nuevaInstancia() {
        return new ConsultaProcedimientoPaso();
    }

    @Override
    protected UUID obtenerId(ConsultaProcedimientoPaso entidad) {
        return entidad.getIdConsultaProcedimientoPaso();
    }
}
