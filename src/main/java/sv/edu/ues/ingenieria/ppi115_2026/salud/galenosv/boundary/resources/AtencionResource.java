package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AutenticacionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ContextoRolService;
import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.AtencionDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;

/** Comparte el contexto de JSF y admite únicamente asignaciones de la cuenta autenticada. */
@Path("atencion")
@RequestScoped
@Transactional(rollbackOn = Exception.class)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AtencionResource {
    @Inject private AtencionSesion sesion;
    @Inject private AccesoSesion acceso;
    @Inject private AutenticacionService autenticacion;
    @Inject private ContextoRolService roles;
    @Context private HttpServletRequest request;

    @GET
    public Response contexto() {
        var activo = sesion.getRolActivo();
        autenticacion.contexto(acceso.getIdUsuario(), activo == null ? null : activo.getIdPersonaRol());
        var json = Json.createObjectBuilder();
        if (sesion.getRolActivo() == null) json.addNull("idPersonaRol");
        else json.add("idPersonaRol", sesion.getRolActivo().getIdPersonaRol().toString());
        if (sesion.getConsulta() == null) json.addNull("idConsulta");
        else json.add("idConsulta", sesion.getConsulta().toString());
        // Se devuelve texto JSON para conservar también los campos nulos.
        return Response.ok(json.build().toString()).build();
    }

    /**
     * Asignaciones de empleado activas de la cuenta, en todas sus clínicas y en el orden de
     * UsuarioRepository.asignaciones(). PermisosApiFilter no limita /atencion, y listar()
     * devuelve [] sin sesión, por eso la sesión se exige aquí también.
     */
    @GET
    @Path("asignaciones")
    public List<AtencionDto.Asignacion> asignaciones() {
        if (!acceso.vigente()) throw new AccesoException(401, "auth.sesion", "Vuelva a iniciar sesión.");
        var activo = sesion.getRolActivo();
        UUID idActivo = activo == null ? null : activo.getIdPersonaRol();
        return roles.listar().stream().map(a -> AtencionDto.Asignacion.de(a, idActivo)).toList();
    }

    @PUT
    @Path("rol-activo")
    public Response cambiarRol(@Valid @NotNull AtencionDto.Responsable datos) {
        var contexto = autenticacion.contexto(acceso.getIdUsuario(), datos.getIdPersonaRol());
        sesion.cambiarRol(contexto.asignacion());
        request.changeSessionId();
        return contexto();
    }
}
