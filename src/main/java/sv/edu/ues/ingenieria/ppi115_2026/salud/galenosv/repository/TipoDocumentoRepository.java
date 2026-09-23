package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import java.util.UUID;

@Stateless
public class TipoDocumentoRepository extends AbstractRepository<TipoDocumento, UUID> {

    public TipoDocumentoRepository() {
        super(TipoDocumento.class);
    }
}
