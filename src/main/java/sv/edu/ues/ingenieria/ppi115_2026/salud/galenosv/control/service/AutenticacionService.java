package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.DocumentoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.PersonaRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.PersonaRolRepository;

@Stateless
public class AutenticacionService {
    @Inject DocumentoRepository documentos;
    @Inject PersonaRepository personas;
    @Inject PersonaRolRepository asignaciones;
    public record Contexto(Persona persona, PersonaRol asignacion) {}
    /** Código del rol de paciente, que conserva permisos limitados a su propio expediente. */
    public static final String PACIENTE = "PACIENTE";

    /**
     * HU2: autentica con un documento de tipo activo y una asignación de empleado activa en
     * una clínica activa.
     */
    public Contexto iniciar(UUID idTipoDocumento, String numeroDocumento) {
        if (idTipoDocumento == null || numeroDocumento == null || numeroDocumento.isBlank()) throw credencialesInvalidas();
        Persona persona = documentos.buscarPersonaPorDocumento(idTipoDocumento, numeroDocumento);
        if (persona == null) throw credencialesInvalidas();
        List<PersonaRol> roles = asignacionesActivas(persona.getIdPersona());
        PersonaRol rol = empleados(roles).findFirst().orElseThrow(this::credencialesInvalidas);
        return new Contexto(persona, rol);
    }

    public Persona personaActiva(UUID idPersona) {
        Persona persona = idPersona == null ? null : personas.find(idPersona);
        if (persona == null || !documentos.tieneDocumentoActivo(idPersona)) {
            throw new AccesoException(401, "auth.sesion", "Vuelva a iniciar sesión.");
        }
        return persona;
    }

    public List<PersonaRol> asignaciones(UUID idPersona) {
        personaActiva(idPersona);
        return empleados(asignacionesActivas(idPersona)).toList();
    }

    public Contexto contexto(UUID idPersona, UUID idPersonaRol) {
        Persona persona = personaActiva(idPersona);
        PersonaRol rol = idPersonaRol == null ? null : asignacionesActivas(idPersona).stream()
                .filter(a -> a.getIdPersonaRol().equals(idPersonaRol)).findFirst().orElse(null);
        if (rol == null) throw sinAsignacion();
        return new Contexto(persona, rol);
    }

    private List<PersonaRol> asignacionesActivas(UUID idPersona) {
        return asignaciones.listarPorPersona(idPersona).stream()
                .filter(a -> a.getIdRol() != null && Boolean.TRUE.equals(a.getIdRol().getActivo())
                        && a.getIdClinica() != null && Boolean.TRUE.equals(a.getIdClinica().getActivo()))
                .toList();
    }

    /**
     * B8: es de empleado cualquier asignación con rol activo cuyo código no sea PACIENTE; un rol
     * sin código (creado desde la app) también lo es. La clínica activa la exige la consulta.
     */
    public static boolean esEmpleado(PersonaRol asignacion) {
        return asignacion != null && asignacion.getIdRol() != null
                && Boolean.TRUE.equals(asignacion.getIdRol().getActivo())
                && !PACIENTE.equals(asignacion.getIdRol().getCodigo());
    }

    /** Defensa adicional a la consulta del repositorio, que aplica la misma regla. */
    private static Stream<PersonaRol> empleados(List<PersonaRol> asignaciones) {
        return asignaciones.stream().filter(AutenticacionService::esEmpleado);
    }

    private AccesoException credencialesInvalidas() {
        return new AccesoException(401, "auth.credenciales", "No encontramos una persona con ese documento activo.");
    }

    private AccesoException sinAsignacion() {
        return new AccesoException(403, "auth.asignacion", "No tiene una asignación activa autorizada. Comuníquese con su clínica.");
    }
}
