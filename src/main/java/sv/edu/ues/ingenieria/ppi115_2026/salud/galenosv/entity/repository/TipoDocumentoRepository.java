package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

@Stateless(name = "TipoDocumentoRepository")
@LocalBean
public class TipoDocumentoRepository extends AbstractRepository<TipoDocumento, UUID> {

    public TipoDocumentoRepository() {
        super(TipoDocumento.class);
    }

    public boolean estaEnUso(UUID id) {
        return em.createQuery("SELECT COUNT(d) FROM Documento d WHERE d.idTipoDocumento.idTipoDocumento=:id", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }
}
