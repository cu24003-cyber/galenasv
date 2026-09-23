package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.DocumentoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import java.util.UUID;


@Stateless
public class DocumentoService extends AbstractService<Documento, UUID> {

    @Inject
    private DocumentoRepository documentoRepository;

    @Override
    protected RepositoryInterface<Documento, UUID> getRepository() {
        return documentoRepository;
    }

    @Override
    protected UUID obtenerId(Documento entidad) {
        return entidad.getIdDocumento();
    }

    @Override
    public void crear(Documento entidad) {
        if (entidad.getIdDocumento() == null) {
            entidad.setIdDocumento(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
