package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AutenticacionService;
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

    @PUT
    @Path("rol-activo")
    public Response cambiarRol(@Valid @NotNull AtencionDto.Responsable datos) {
        var contexto = autenticacion.contexto(acceso.getIdUsuario(), datos.getIdPersonaRol());
        sesion.cambiarRol(contexto.asignacion());
        request.changeSessionId();
        return contexto();
    }
}
