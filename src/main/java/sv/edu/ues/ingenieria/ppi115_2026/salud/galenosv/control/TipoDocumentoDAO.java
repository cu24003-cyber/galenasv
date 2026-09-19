package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import java.util.UUID;

@Stateless
public class TipoDocumentoDAO extends DefaultDAO<TipoDocumento, UUID> {

    public TipoDocumentoDAO() {
        super(TipoDocumento.class);
    }
}
