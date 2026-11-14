package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Usuario;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.UsuarioRepository;

@Stateless
public class AutenticacionService {
    @Inject UsuarioRepository usuarios;
    public record Contexto(Usuario usuario, PersonaRol asignacion) {}
    /** Código del único rol que no es de empleado: el paciente nunca inicia sesión (HU2). */
    public static final String PACIENTE = "PACIENTE";

    /**
     * HU2: login de empleados por tipo y número de documento, sin contraseña. Todo rechazo
     * usa el mismo 401 para no revelar qué documentos existen ni a quién pertenecen.
     */
    public Contexto iniciar(UUID idTipoDocumento, String numeroDocumento) {
        if (idTipoDocumento == null || numeroDocumento == null || numeroDocumento.isBlank()) throw credencialesInvalidas();
        Usuario usuario = usuarios.buscarPorDocumento(idTipoDocumento, numeroDocumento);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) throw credencialesInvalidas();
        // La consulta ya viene ordenada por antigüedad; se toma la primera asignación de empleado.
        return empleados(usuarios.asignaciones(usuario.getPersona().getIdPersona())).findFirst()
                .map(asignacion -> new Contexto(usuario, asignacion))
                .orElseThrow(this::credencialesInvalidas);
    }

    public Usuario usuarioActivo(UUID idUsuario) {
        Usuario usuario = idUsuario == null ? null : usuarios.buscarSesion(idUsuario);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AccesoException(401, "auth.sesion", "Vuelva a iniciar sesión.");
        }
        return usuario;
    }

    public List<PersonaRol> asignaciones(UUID idUsuario) {
        return usuarios.asignaciones(usuarioActivo(idUsuario).getPersona().getIdPersona());
    }

    public Contexto contexto(UUID idUsuario, UUID idPersonaRol) {
        Usuario usuario = usuarioActivo(idUsuario);
        PersonaRol rol = empleados(usuarios.asignaciones(usuario.getPersona().getIdPersona()))
                .filter(a -> a.getIdPersonaRol().equals(idPersonaRol)).findFirst().orElseThrow(this::sinAsignacion);
        return new Contexto(usuario, rol);
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
        return new AccesoException(401, "auth.credenciales", "Documento no registrado o sin acceso autorizado.");
    }

    private AccesoException sinAsignacion() {
        return new AccesoException(403, "auth.asignacion", "No tiene una asignación activa autorizada. Comuníquese con su clínica.");
    }
}
