package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.DocumentoService;

@Named("documentoModel")
@ViewScoped
public class DocumentoModel extends AbstractModel<Documento, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private DocumentoService documentoService;

    @Override
    protected DocumentoService getService() {
        return documentoService;
    }

    @Override
    protected Documento nuevaInstancia() {
        return new Documento();
    }

    @Override
    protected UUID obtenerId(Documento entidad) {
        return entidad.getIdDocumento();
    }
}
