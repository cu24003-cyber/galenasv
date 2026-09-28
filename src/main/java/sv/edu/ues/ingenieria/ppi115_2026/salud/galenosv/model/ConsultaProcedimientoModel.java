package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimiento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ConsultaProcedimientoService;

@Named("consultaProcedimientoModel")
@ViewScoped
public class ConsultaProcedimientoModel extends AbstractModel<ConsultaProcedimiento, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ConsultaProcedimientoService consultaProcedimientoService;

    @Override
    protected ConsultaProcedimientoService getService() {
        return consultaProcedimientoService;
    }

    @Override
    protected ConsultaProcedimiento nuevaInstancia() {
        return new ConsultaProcedimiento();
    }

    @Override
    protected UUID obtenerId(ConsultaProcedimiento entidad) {
        return entidad.getIdConsultaProcedimiento();
    }
}
