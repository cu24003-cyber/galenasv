package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Consulta;
import java.util.List;
import java.util.UUID;

@Stateless
public class ConsultaDAO extends DefaultDAO<Consulta, UUID> {

    public ConsultaDAO() {
        super(Consulta.class);
    }

    public List<Consulta> listarPorPersona(UUID idPersona) {
        return em.createQuery(
                        "SELECT c FROM Consulta c WHERE c.idPersonaRol.idPersona.idPersona = :idPersona",
                        Consulta.class)
                .setParameter("idPersona", idPersona)
                .getResultList();
    }
}
