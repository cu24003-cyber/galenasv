package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

/** Mantiene separadas las asignaciones autenticadas de la lista global del selector JSF. */
@Stateless
public class ContextoRolService {
    @jakarta.inject.Inject private AccesoSesion acceso;
    @jakarta.inject.Inject private AutenticacionService autenticacion;
    @PersistenceContext private EntityManager em;

    public List<PersonaRol> listar() {
        return acceso.vigente() ? autenticacion.asignaciones(acceso.getIdPersona()) : List.of();
    }

    /** Clínicas activas registradas, aunque la cuenta autenticada no tenga una asignación allí. */
    public List<Clinica> listarClinicasParaCambio() {
        return em.createQuery("SELECT c FROM Clinica c WHERE c.activo=true ORDER BY c.nombre", Clinica.class)
                .getResultList();
    }

    /** Asignaciones activas de todas las personas para el selector de la aplicación JSF. */
    public List<PersonaRol> listarParaCambio() {
        return em.createQuery("SELECT a FROM PersonaRol a JOIN FETCH a.idPersona p "
                + "JOIN FETCH a.idRol r JOIN FETCH a.idClinica c WHERE r.activo=true AND c.activo=true "
                + "ORDER BY p.apellidos, p.nombres, c.nombre, r.nombre", PersonaRol.class).getResultList();
    }

    public PersonaRol cargarParaCambio(UUID id) {
        return listarParaCambio().stream().filter(r -> r.getIdPersonaRol().equals(id)).findFirst()
                .orElseThrow(() -> ServiceException.localizada("rolCambio.asignacionActiva",
                        "Seleccione una asignación con un rol activo."));
    }

    public PersonaRol cargar(UUID id) {
        return listar().stream().filter(r -> r.getIdPersonaRol().equals(id)).findFirst()
                .orElseThrow(() -> ServiceException.localizada("rolCambio.asignacionActiva", "Seleccione una asignación con un rol activo."));
    }
}
