package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoExamenService;

@Named("tipoExamenModel")
@ViewScoped
public class TipoExamenModel extends AbstractModel<TipoExamen, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private TipoExamenService tipoExamenService;

    @Override
    protected TipoExamenService getService() {
        return tipoExamenService;
    }

    @Override
    protected TipoExamen nuevaInstancia() {
        return new TipoExamen();
    }

    @Override
    protected UUID obtenerId(TipoExamen entidad) {
        return entidad.getIdTipoExamen();
    }
}
