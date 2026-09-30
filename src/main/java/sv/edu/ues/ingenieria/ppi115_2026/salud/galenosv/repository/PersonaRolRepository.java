package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import java.util.UUID;

@Stateless
public class PersonaRolRepository extends AbstractRepository<PersonaRol, UUID> {

    public PersonaRolRepository() {
        super(PersonaRol.class);
    }
    public java.util.List<PersonaRol> listarPorPersona(UUID persona) {
        return em.createQuery("SELECT pr FROM PersonaRol pr JOIN FETCH pr.idRol LEFT JOIN FETCH pr.idClinica "
                + "WHERE pr.idPersona.idPersona = :persona ORDER BY pr.fechaCreacion, pr.idPersonaRol", PersonaRol.class)
                .setParameter("persona", persona).getResultList();
    }

    public java.util.List<sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona> listarPacientes() {
        return em.createQuery("SELECT DISTINCT p FROM PersonaRol pr JOIN pr.idPersona p JOIN pr.idRol r "
                + "WHERE LOWER(TRIM(r.nombre)) = :rol ORDER BY p.apellidos, p.nombres",
                sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona.class)
                .setParameter("rol", "paciente").getResultList();
    }
}
