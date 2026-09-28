package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.MedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.MedioContactoService;

@Named("medioContactoModel")
@ViewScoped
public class MedioContactoModel extends AbstractModel<MedioContacto, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private MedioContactoService medioContactoService;

    @Override
    protected MedioContactoService getService() {
        return medioContactoService;
    }

    @Override
    protected MedioContacto nuevaInstancia() {
        return new MedioContacto();
    }

    @Override
    protected UUID obtenerId(MedioContacto entidad) {
        return entidad.getIdMedioContacto();
    }
}
