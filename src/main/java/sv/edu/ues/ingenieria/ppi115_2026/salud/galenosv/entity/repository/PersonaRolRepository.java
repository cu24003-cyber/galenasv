package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

@Stateless
public class PersonaRolRepository extends AbstractRepository<PersonaRol, UUID> {

    public PersonaRolRepository() {
        super(PersonaRol.class);
    }
    public java.util.List<PersonaRol> listarPorPersona(UUID persona) {
        return em.createQuery("SELECT pr FROM PersonaRol pr LEFT JOIN FETCH pr.idRol LEFT JOIN FETCH pr.idClinica "
                + "WHERE pr.idPersona.idPersona = :persona ORDER BY pr.fechaCreacion, pr.idPersonaRol", PersonaRol.class)
                .setParameter("persona", persona).getResultList();
    }

    public java.util.List<sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona> listarPacientes() {
        return em.createQuery("SELECT DISTINCT p FROM PersonaRol pr JOIN pr.idPersona p JOIN pr.idRol r "
                + "WHERE LOWER(TRIM(r.nombre)) = :rol ORDER BY p.apellidos, p.nombres",
                sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona.class)
                .setParameter("rol", "paciente").getResultList();
    }

    /** Persona + rol (o sin rol) + clínica identifican una asignación; excluir es la que se edita. */
    public boolean existeAsignacion(UUID persona, UUID rol, UUID clinica, UUID excluir) {
        String jpql = "SELECT COUNT(pr) FROM PersonaRol pr WHERE pr.idPersona.idPersona=:persona"
                + " AND pr.idClinica.idClinica=:clinica"
                + (rol == null ? " AND pr.idRol IS NULL" : " AND pr.idRol.idRol=:rol")
                + (excluir == null ? "" : " AND pr.idPersonaRol<>:excluir");
        var consulta = em.createQuery(jpql, Long.class).setParameter("persona", persona).setParameter("clinica", clinica);
        if (rol != null) consulta.setParameter("rol", rol);
        if (excluir != null) consulta.setParameter("excluir", excluir);
        return consulta.getSingleResult() > 0;
    }
}
