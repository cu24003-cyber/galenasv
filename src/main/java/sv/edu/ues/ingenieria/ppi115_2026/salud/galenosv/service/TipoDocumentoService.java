package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.TipoDocumentoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import java.util.UUID;


@Stateless
public class TipoDocumentoService extends AbstractService<TipoDocumento, UUID> {

    @Inject
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Override
    protected RepositoryInterface<TipoDocumento, UUID> getRepository() {
        return tipoDocumentoRepository;
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
