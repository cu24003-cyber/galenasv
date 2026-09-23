package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.TipoDocumentoDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import java.util.UUID;


@Stateless
public class TipoDocumentoService extends AbstractService<TipoDocumento, UUID> {

    @Inject
    private TipoDocumentoDAO tipoDocumentoDAO;

    @Override
    protected DefaultDAOInterface<TipoDocumento, UUID> getRepository() {
        return tipoDocumentoDAO;
    }

    @Override
    protected UUID obtenerId(TipoDocumento entidad) {
        return entidad.getIdTipoDocumento();
    }

    @Override
    public void crear(TipoDocumento entidad) {
        if (entidad.getIdTipoDocumento() == null) {
            entidad.setIdTipoDocumento(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
