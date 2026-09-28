package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ExamenService;

@Named("examenModel")
@ViewScoped
public class ExamenModel extends AbstractModel<Examen, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ExamenService examenService;

    @Override
    protected ExamenService getService() {
        return examenService;
    }

    @Override
    protected Examen nuevaInstancia() {
        return new Examen();
    }

    @Override
    protected UUID obtenerId(Examen entidad) {
        return entidad.getIdExamen();
    }
}
