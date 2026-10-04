package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.OrdenExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.OrdenExamen;

@Named("ordenExamenModel")
@ViewScoped
public class OrdenExamenModel extends AbstractModel<OrdenExamen, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private OrdenExamenService ordenExamenService;

    @Override
    protected OrdenExamenService getService() {
        return ordenExamenService;
    }

    @Override
    protected OrdenExamen nuevaInstancia() {
        return new OrdenExamen();
    }

    @Override
    protected UUID obtenerId(OrdenExamen entidad) {
        return entidad.getIdOrdenExamen();
    }
}
