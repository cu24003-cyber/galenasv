package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;

@Path("auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject AutenticacionService servicio;
    @Inject AccesoSesion acceso;
    @Inject AtencionSesion atencion;
    @Inject IntentosAcceso intentos;
    @Context HttpServletRequest request;

    /** HU2: el empleado se identifica con tipo y número de documento, sin contraseña. */
    public static class Credenciales {
        @NotNull public UUID idTipoDocumento;
        @NotBlank @Size(max = 50) @Pattern(regexp = "[^\\r\\n]*") public String numeroDocumento;
    }

    @POST @Path("login") @Consumes(MediaType.APPLICATION_JSON)
    public Response login(@Valid @NotNull Credenciales datos) {
        intentos.registrar(request.getRemoteAddr());
        var contexto = servicio.iniciar(datos.idTipoDocumento, datos.numeroDocumento);
        var anterior = request.getSession(false);
        if (anterior != null) anterior.invalidate();
        request.getSession(true);
        request.changeSessionId();
        acceso.autenticar(contexto.usuario().getIdUsuario());
        atencion.cambiarRol(contexto.asignacion());
        return Response.noContent().build();
    }

    @GET @Path("me")
    public Response me() {
        exigirSesion();
        var activo = atencion.getRolActivo();
        AutenticacionService.Contexto contexto;
        try {
            contexto = servicio.contexto(acceso.getIdUsuario(), activo == null ? null : activo.getIdPersonaRol());
        } catch (AccesoException error) {
            request.getSession(false).invalidate();
            throw error;
        }
        var usuario = contexto.usuario();
        var persona = usuario.getPersona();
        var rol = contexto.asignacion();
        var body = Json.createObjectBuilder()
                .add("usuario", Json.createObjectBuilder()
                        .add("idUsuario", usuario.getIdUsuario().toString())
                        .add("idPersona", persona.getIdPersona().toString())
                        .add("nombres", persona.getNombres() == null || persona.getNombres().isBlank() ? usuario.getNombreUsuario() : persona.getNombres())
                        .add("apellidos", persona.getApellidos() == null ? "" : persona.getApellidos()))
                .add("rolActivo", rolActivo(rol))
                .add("csrfToken", acceso.getCsrfToken()).build();
        return Response.ok(body.toString()).build();
    }

    /** B8: cualquier rol de empleado puede ser el activo; un rol creado sin código devuelve codigo null. */
    private static jakarta.json.JsonObjectBuilder rolActivo(sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol rol) {
        var json = Json.createObjectBuilder().add("idPersonaRol", rol.getIdPersonaRol().toString());
        if (rol.getIdRol().getCodigo() == null) json.addNull("codigo");
        else json.add("codigo", rol.getIdRol().getCodigo());
        if (rol.getIdRol().getNombre() == null) json.addNull("nombreRol");
        else json.add("nombreRol", rol.getIdRol().getNombre());
        return json.add("idClinica", rol.getIdClinica().getIdClinica().toString())
                .add("nombreClinica", rol.getIdClinica().getNombre());
    }

    @POST @Path("logout")
    public Response logout() {
        exigirSesion();
        request.getSession(false).invalidate();
        return Response.noContent().build();
    }

    private void exigirSesion() {
        if (request.getSession(false) == null || !acceso.vigente()) {
            throw new AccesoException(401, "auth.sesion", "Vuelva a iniciar sesión.");
        }
    }
}
