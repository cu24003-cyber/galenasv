package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenResultadoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ExamenResultado;

@Named("examenResultadoModel")
@ViewScoped
public class ExamenResultadoModel extends AbstractModel<ExamenResultado, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ExamenResultadoService examenResultadoService;

    @Override
    protected ExamenResultadoService getService() {
        return examenResultadoService;
    }

    @Override
    protected ExamenResultado nuevaInstancia() {
        return new ExamenResultado();
    }

    @Override
    protected UUID obtenerId(ExamenResultado entidad) {
        return entidad.getIdExamenResultado();
    }
}
