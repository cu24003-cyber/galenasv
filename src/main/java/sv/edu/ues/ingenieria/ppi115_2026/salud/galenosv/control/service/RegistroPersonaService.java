package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.PersonaRolRepository;

/** Guarda persona y asignación en una sola transacción. */
@Stateless
public class RegistroPersonaService {
    @EJB private PersonaService personas;
    @EJB private RolService roles;
    @EJB private ClinicaService clinicas;
    @EJB private PersonaRolService asignaciones;
    @EJB private PersonaRolRepository repositorio;

    public List<PersonaRol> listarPorPersona(UUID persona) { return repositorio.listarPorPersona(persona); }
    public List<Persona> listarPacientes() { return repositorio.listarPacientes(); }

    public UUID guardar(Persona persona, Rol rol, Clinica clinica, UUID asignacionId) {
        if (rol == null && clinica == null) {
            // Omitir la asignación no elimina relaciones ya utilizadas por consultas o pasos.
            if (asignacionId != null) {
                PersonaRol actual = asignaciones.buscarPorId(asignacionId);
                if (actual == null || !persona.equals(actual.getIdPersona()))
                    throw new ServiceException("La asignación no pertenece a la persona.");
            }
            guardarPersona(persona);
            return asignacionId;
        }
        Rol existente = rol == null ? null : roles.buscarPorId(rol.getIdRol());
        Clinica sede = clinica == null ? null : clinicas.buscarPorId(clinica.getIdClinica());
        if ((rol != null && (existente == null || !Boolean.TRUE.equals(existente.getActivo())))
                || sede == null || !Boolean.TRUE.equals(sede.getActivo())) {
            throw new ServiceException("registro.seleccionInvalida", "Seleccione un rol y una clínica activos.", null);
        }
        PersonaRol asignacion = asignacionId == null ? new PersonaRol() : asignaciones.buscarPorId(asignacionId);
        if (asignacion == null || (asignacionId != null
                && !persona.equals(asignacion.getIdPersona()))) {
            throw new ServiceException("La asignación no pertenece a la persona.");
        }
        guardarPersona(persona);
        asignacion.setIdPersona(persona);
        // Una pertenencia a clínica puede existir sin rol; omitir uno existente no lo borra.
        if (existente != null || asignacion.getIdRol() == null) asignacion.setIdRol(existente);
        asignacion.setIdClinica(sede);
        if (asignacionId == null) asignaciones.crear(asignacion);
        else asignaciones.actualizar(asignacion);
        return asignacion.getIdPersonaRol();
    }
    private void guardarPersona(Persona persona) {
        if (persona.getIdPersona() == null) personas.crear(persona);
        else personas.actualizar(persona);
    }
}
