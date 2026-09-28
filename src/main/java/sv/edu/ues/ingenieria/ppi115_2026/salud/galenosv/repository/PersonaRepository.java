package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import java.util.UUID;
import java.util.List;
import java.util.Locale;

@Stateless
public class PersonaRepository extends AbstractRepository<Persona, UUID> {

    public List<Persona> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return findAll();
        }
        return em.createQuery(
                "SELECT p FROM Persona p WHERE LOCATE(:nombre, LOWER(p.nombres)) > 0 "
                        + "ORDER BY p.nombres, p.apellidos, p.idPersona", Persona.class)
                .setParameter("nombre", nombre.strip().toLowerCase(Locale.ROOT))
                .getResultList();
    }

    public PersonaRepository() {
        super(Persona.class);
    }
}
