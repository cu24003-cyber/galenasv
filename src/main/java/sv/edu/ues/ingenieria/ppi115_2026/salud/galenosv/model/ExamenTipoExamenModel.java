package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenTipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ExamenTipoExamenService;

@Named("examenTipoExamenModel")
@ViewScoped
public class ExamenTipoExamenModel extends AbstractModel<ExamenTipoExamen, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ExamenTipoExamenService examenTipoExamenService;

    @Override
    protected ExamenTipoExamenService getService() {
        return examenTipoExamenService;
    }

    @Override
    protected ExamenTipoExamen nuevaInstancia() {
        return new ExamenTipoExamen();
    }

    @Override
    protected UUID obtenerId(ExamenTipoExamen entidad) {
        return entidad.getIdExamenTipoExamen();
    }
}
