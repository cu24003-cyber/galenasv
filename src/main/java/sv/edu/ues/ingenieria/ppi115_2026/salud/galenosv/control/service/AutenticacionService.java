package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Usuario;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.UsuarioRepository;

@Stateless
public class AutenticacionService {
    @Inject UsuarioRepository usuarios;
    @Inject ContrasenaService contrasenas;
    public record Contexto(Usuario usuario, PersonaRol asignacion) {}

    public Contexto iniciar(String identificador, String password) {
        Usuario usuario = usuarios.buscarAcceso(identificador);
        boolean correcta = contrasenas.verificar(password, usuario == null ? contrasenas.hashAusente() : usuario.getPasswordHash());
        if (!correcta || usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AccesoException(401, "auth.credenciales", "Usuario o contraseña incorrectos.");
        }
        List<PersonaRol> roles = usuarios.asignaciones(usuario.getPersona().getIdPersona());
        if (roles.isEmpty()) throw sinAsignacion();
        // Se usa la asignación más antigua; cambiar de rol solo admite asignaciones propias.
        return new Contexto(usuario, roles.getFirst());
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
        PersonaRol rol = usuarios.asignaciones(usuario.getPersona().getIdPersona()).stream()
                .filter(a -> a.getIdPersonaRol().equals(idPersonaRol)).findFirst().orElseThrow(this::sinAsignacion);
        return new Contexto(usuario, rol);
    }

    private AccesoException sinAsignacion() {
        return new AccesoException(403, "auth.asignacion", "No tiene una asignación activa autorizada. Comuníquese con su clínica.");
    }
}
