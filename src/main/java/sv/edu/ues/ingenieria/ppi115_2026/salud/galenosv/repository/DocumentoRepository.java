package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import java.util.UUID;

@Stateless
public class DocumentoRepository extends AbstractRepository<Documento, UUID> {

    public DocumentoRepository() {
        super(Documento.class);
    }
}
