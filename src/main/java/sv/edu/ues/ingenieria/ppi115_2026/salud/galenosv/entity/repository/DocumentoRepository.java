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

    /** Busca una persona por el documento exacto, si su tipo está activo. */
    public Persona buscarPersonaPorDocumento(UUID idTipoDocumento, String valor) {
        if (idTipoDocumento == null || valor == null || valor.isBlank()) return null;
        var personas = em.createQuery("SELECT DISTINCT p FROM Documento d JOIN d.idPersona p JOIN d.idTipoDocumento t "
                + "WHERE t.idTipoDocumento=:tipo AND t.activo=true AND d.valor=:valor", Persona.class)
                .setParameter("tipo", idTipoDocumento).setParameter("valor", valor.strip())
                .setMaxResults(2).getResultList();
        // Sin UNIQUE en la base original, un documento ambiguo nunca identifica a una persona.
        return personas.size() == 1 ? personas.get(0) : null;
    }

    /** Una sesión deja de ser válida si la persona pierde todos sus tipos de documento activos. */
    public boolean tieneDocumentoActivo(UUID idPersona) {
        return em.createQuery("SELECT COUNT(d) FROM Documento d WHERE d.idPersona.idPersona=:persona "
                + "AND d.idTipoDocumento.activo=true", Long.class)
                .setParameter("persona", idPersona).getSingleResult() > 0;
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

    /** Sin UNIQUE en la base original: tipo + valor (sin espacios exteriores) solo puede tener un dueño. */
    public boolean existeValorEnOtraPersona(UUID tipo, String valor, UUID persona) {
        String jpql = "SELECT COUNT(d) FROM Documento d WHERE d.idTipoDocumento.idTipoDocumento=:tipo"
                + " AND TRIM(d.valor)=:valor"
                + (persona == null ? "" : " AND (d.idPersona IS NULL OR d.idPersona.idPersona<>:persona)");
        var consulta = em.createQuery(jpql, Long.class).setParameter("tipo", tipo).setParameter("valor", valor);
        if (persona != null) consulta.setParameter("persona", persona);
        return consulta.getSingleResult() > 0;
    }
}
