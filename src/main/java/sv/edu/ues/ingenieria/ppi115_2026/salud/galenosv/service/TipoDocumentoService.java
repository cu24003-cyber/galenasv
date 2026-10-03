package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.ejb.LocalBean;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.TipoDocumentoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import java.util.UUID;


@Stateless(name = "TipoDocumentoService")
@LocalBean
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

    public boolean estaEnUso(UUID id) {
        return id != null && tipoDocumentoRepository.estaEnUso(id);
    }

    @Override
    public void actualizar(TipoDocumento entidad) {
        validar(entidad);
        if (estaEnUso(entidad.getIdTipoDocumento())) {
            throw ServiceException.localizada("tipoDocumento.enUso",
                    "No se puede editar un tipo de documento que ya tiene documentos asociados.");
        }
        super.actualizar(entidad);
    }

}
