package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoMedioContactoService;

@Named("tipoMedioContactoModel")
@ViewScoped
public class TipoMedioContactoModel extends AbstractModel<TipoMedioContacto, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private TipoMedioContactoService tipoMedioContactoService;

    @Override
    protected TipoMedioContactoService getService() {
        return tipoMedioContactoService;
    }

    @Override
    protected TipoMedioContacto nuevaInstancia() {
        return new TipoMedioContacto();
    }

    @Override
    protected UUID obtenerId(TipoMedioContacto entidad) {
        return entidad.getIdTipoMedioContacto();
    }
}
