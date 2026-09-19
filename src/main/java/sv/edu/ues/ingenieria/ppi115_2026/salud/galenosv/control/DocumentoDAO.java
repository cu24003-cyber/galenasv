package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import java.util.UUID;

@Stateless
public class DocumentoDAO extends DefaultDAO<Documento, UUID> {

    public DocumentoDAO() {
        super(Documento.class);
    }
}
