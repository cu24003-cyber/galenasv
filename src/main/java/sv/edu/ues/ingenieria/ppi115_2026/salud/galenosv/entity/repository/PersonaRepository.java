package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

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
