package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoDocumentoService;

@Named("tipoDocumentoModel")
@ViewScoped
public class TipoDocumentoModel extends AbstractModel<TipoDocumento, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private TipoDocumentoService tipoDocumentoService;

    @Override
    protected TipoDocumentoService getService() {
        return tipoDocumentoService;
    }

    @Override
    protected TipoDocumento nuevaInstancia() {
        return new TipoDocumento();
    }

    @Override
    protected UUID obtenerId(TipoDocumento entidad) {
        return entidad.getIdTipoDocumento();
    }
}
