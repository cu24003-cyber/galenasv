package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;

/** Consulta las asignaciones existentes sin crear usuarios ni persistir la sesión. */
@Stateless
public class ContextoRolService {
    @PersistenceContext private EntityManager em;

    public List<PersonaRol> listar() {
        return em.createQuery("SELECT r FROM PersonaRol r JOIN FETCH r.idPersona p JOIN FETCH r.idRol rol JOIN FETCH r.idClinica c WHERE rol.activo=true AND c.activo=true ORDER BY p.apellidos, p.nombres, c.nombre, rol.nombre", PersonaRol.class).getResultList();
    }

    public PersonaRol cargar(UUID id) {
        return listar().stream().filter(r -> r.getIdPersonaRol().equals(id)).findFirst()
                .orElseThrow(() -> new ServiceException("Seleccione una asignación con un rol activo."));
    }
}
