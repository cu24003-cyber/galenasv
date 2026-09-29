package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import java.util.UUID;

@Stateless
public class DocumentoRepository extends AbstractRepository<Documento, UUID> {

    public DocumentoRepository() {
        super(Documento.class);
    }
    public java.util.List<Documento> listarPorPersona(UUID idPersona) {
        return em.createQuery("SELECT e FROM Documento e LEFT JOIN FETCH e.idTipoDocumento "
                + "WHERE e.idPersona.idPersona = :persona", Documento.class)
                .setParameter("persona", idPersona).getResultList();
    }
}
