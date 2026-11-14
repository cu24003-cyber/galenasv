package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AutenticacionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ContextoRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.DashboardService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.DashboardDto;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
    @Inject private DashboardService dashboardService;
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

    /**
     * HU3: consultas de la clínica del rol activo. desde/hasta son días yyyy-MM-dd de El Salvador;
     * se reciben como texto para que una fecha mal escrita dé 400 dashboard.fecha del servicio.
     */
    @GET
    @Path("dashboard")
    public Response dashboard(@QueryParam("desde") String desde, @QueryParam("hasta") String hasta) {
        return Response.ok(DashboardDto.consultas(dashboardService.consultas(desde, hasta)).toString()).build();
    }

    /**
     * HU3/HU4: la consulta seleccionada, con la misma forma que su elemento del dashboard. Solo
     * lee: para atenderla se sigue usando PUT /api/consultas/{id}/sesion.
     */
    @GET
    @Path("consultas/{idConsulta}")
    public Response consulta(@PathParam("idConsulta") UUID idConsulta) {
        return Response.ok(DashboardDto.consulta(dashboardService.consulta(idConsulta)).toString()).build();
    }

    /** HU3: lo que se registró en un paso, solo cuando el usuario lo abre. */
    @GET
    @Path("dashboard/pasos/{idPaso}/valor")
    public Response valorPaso(@PathParam("idPaso") UUID idPaso) {
        return Response.ok(DashboardDto.valor(dashboardService.valor(idPaso)).toString()).build();
    }

    /**
     * B8, HU4: sale de la consulta en curso sin cerrarla. La consulta queda abierta y sin cambios;
     * después se puede cambiar de rol o seleccionar otra. Idempotente: sin consulta en curso
     * también responde 204. Exige sesión (401) y revalida la asignación activa (401/403).
     */
    @DELETE
    @Path("consulta")
    public Response salirDeConsulta() {
        if (!acceso.vigente()) throw new AccesoException(401, "auth.sesion", "Vuelva a iniciar sesión.");
        var activo = sesion.getRolActivo();
        autenticacion.contexto(acceso.getIdUsuario(), activo == null ? null : activo.getIdPersonaRol());
        sesion.setConsulta(null);
        return Response.noContent().build();
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
