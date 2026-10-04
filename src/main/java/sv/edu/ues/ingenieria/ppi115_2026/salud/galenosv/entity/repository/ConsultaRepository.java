package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Consulta;

@Stateless
public class ConsultaRepository extends AbstractRepository<Consulta, UUID> {

    public ConsultaRepository() {
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
