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
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
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

    /** HU2: la persona se identifica con tipo y número de documento, sin contraseña. */
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
        acceso.autenticar(contexto.persona().getIdPersona());
        if (contexto.asignacion() != null) atencion.cambiarRol(contexto.asignacion());
        return Response.noContent().build();
    }

    @GET @Path("me")
    public Response me() {
        exigirSesion();
        Persona persona;
        PersonaRol rol = atencion.getRolActivo();
        try {
            persona = servicio.personaActiva(acceso.getIdPersona());
            if (rol != null) rol = servicio.contexto(acceso.getIdPersona(), rol.getIdPersonaRol()).asignacion();
        } catch (AccesoException error) {
            request.getSession(false).invalidate();
            throw error;
        }
        var body = Json.createObjectBuilder()
                .add("usuario", Json.createObjectBuilder()
                        // Compatibilidad del contrato: idUsuario ahora identifica la persona; no hay tabla usuario.
                        .add("idUsuario", persona.getIdPersona().toString())
                        .add("idPersona", persona.getIdPersona().toString())
                        .add("nombres", persona.getNombres() == null ? "" : persona.getNombres())
                        .add("apellidos", persona.getApellidos() == null ? "" : persona.getApellidos()));
        if (rol == null) body.addNull("rolActivo");
        else body.add("rolActivo", rolActivo(rol).build());
        body.add("csrfToken", acceso.getCsrfToken());
        return Response.ok(body.build().toString()).build();
    }

    /** Un rol de empleado devuelve codigo null si fue creado sin código; una persona sin rol devuelve null. */
    private static jakarta.json.JsonObjectBuilder rolActivo(sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol rol) {
        var json = Json.createObjectBuilder().add("idPersonaRol", rol.getIdPersonaRol().toString())
                .add("idRol", rol.getIdRol().getIdRol().toString());
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
