package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DocumentoDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import java.util.UUID;


@Stateless
public class DocumentoService extends AbstractService<Documento, UUID> {

    @Inject
    private DocumentoDAO documentoDAO;

    @Override
    protected DefaultDAOInterface<Documento, UUID> getDao() {
        return documentoDAO;
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
