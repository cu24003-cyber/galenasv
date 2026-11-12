package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

@Stateless(name = "TipoDocumentoRepository")
@LocalBean
public class TipoDocumentoRepository extends AbstractRepository<TipoDocumento, UUID> {

    public TipoDocumentoRepository() {
        super(TipoDocumento.class);
    }

    public List<TipoDocumento> listarActivos() {
        return em.createQuery("SELECT t FROM TipoDocumento t WHERE t.activo=true ORDER BY t.nombre", TipoDocumento.class)
                .getResultList();
    }

    public boolean estaEnUso(UUID id) {
        return em.createQuery("SELECT COUNT(d) FROM Documento d WHERE d.idTipoDocumento.idTipoDocumento=:id", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }
}
