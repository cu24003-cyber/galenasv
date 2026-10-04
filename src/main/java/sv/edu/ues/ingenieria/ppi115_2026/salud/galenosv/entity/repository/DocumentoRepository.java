package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

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

    public Persona bloquearPersona(UUID id) {
        return em.find(Persona.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public boolean existeDuiPorPersona(UUID persona, UUID excluir) {
        String jpql = "SELECT COUNT(d) FROM Documento d WHERE d.idPersona.idPersona=:persona"
                + " AND LOWER(TRIM(d.idTipoDocumento.nombre)) IN ('dui', 'documento único de identidad', 'documento unico de identidad')"
                + (excluir == null ? "" : " AND d.idDocumento<>:excluir");
        var consulta = em.createQuery(jpql, Long.class).setParameter("persona", persona);
        if (excluir != null) consulta.setParameter("excluir", excluir);
        return consulta.getSingleResult() > 0;
    }
}
