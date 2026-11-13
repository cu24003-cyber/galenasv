package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.AtencionDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ConsultaProcedimientoPasoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimientoPaso;

@Path("consultas-procedimientos-pasos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ConsultaProcedimientoPasoResource extends AbstractResource<ConsultaProcedimientoPaso, ConsultaProcedimientoPasoDto> {

    @Inject
    private ConsultaProcedimientoPasoService service;

    @Inject private AtencionService atencion;

    @Override
    protected Set<String> metodosEscritura() { return idPadre() == null ? Set.of("POST", "PUT", "DELETE") : Set.of(); }

    @PUT
    @Path("{id}/responsable")
    public Response asignar(@PathParam("id") String id, @Valid @NotNull AtencionDto.Responsable datos) {
        ConsultaProcedimientoPaso paso = buscarExistente(parseId(id));
        atencion.asignarResponsable(consultaDe(paso), paso.getIdConsultaProcedimientoPaso(), datos.getIdPersonaRol());
        return Response.ok(toDto(paso)).build();
    }

    @PUT
    @Path("{id}/finalizacion")
    public Response completar(@PathParam("id") String id, @Valid @NotNull AtencionDto.Finalizacion datos) {
        ConsultaProcedimientoPaso paso = buscarExistente(parseId(id));
        atencion.completar(consultaDe(paso), paso.getIdConsultaProcedimientoPaso(), datos.getValor());
        return Response.ok(toDto(paso)).build();
    }

    /** HU4: reabre el paso anterior y elimina este paso vacío. Responde con el anterior reabierto. */
    @PUT
    @Path("{id}/retroceso")
    public Response retroceder(@PathParam("id") String id) {
        ConsultaProcedimientoPaso paso = buscarExistente(parseId(id));
        ConsultaProcedimientoPaso anterior = atencion.retroceder(consultaDe(paso), paso.getIdConsultaProcedimientoPaso());
        return Response.ok(toDto(anterior)).build();
    }

    @POST
    @Path("{id}/examenes")
    public Response registrarExamen(@PathParam("id") String id, @Valid @NotNull AtencionDto.Examen datos,
            @Context UriInfo uriInfo, @Context ResourceContext recursos) {
        ConsultaProcedimientoPaso paso = buscarExistente(parseId(id));
        var examen = atencion.examen(consultaDe(paso), paso.getIdConsultaProcedimientoPaso(),
                datos.getNombre(), datos.getObservaciones(), datos.getIdTipoExamen());
        return Response.created(uriInfo.getBaseUriBuilder().path("examenes").path(examen.getIdExamen().toString()).build())
                .entity(recursos.getResource(ExamenResource.class).toDto(examen)).build();
    }

    private UUID consultaDe(ConsultaProcedimientoPaso paso) {
        return paso.getIdConsultaProcedimiento().getIdConsulta().getIdConsulta();
    }

    @Inject
    private ConsultaProcedimientoService consultaProcedimientoService;

    @Inject
    private PersonaRolService personaRolService;

    @Inject
    private ProcedimientoPasoService procedimientoPasoService;

    @Override
    protected Map<String, String> filtrosPermitidos() {
        return Map.of(
                "idConsultaProcedimiento", "idConsultaProcedimiento.idConsultaProcedimiento",
                "idPersonaRol", "idPersonaRol.idPersonaRol",
                "idProcedimientoPaso", "idProcedimientoPaso.idProcedimientoPaso");
    }

    @Override
    protected Map<String, ColeccionRelacionada> coleccionesRelacionadas() {
        return Map.of(
                "ordenes-examen", new ColeccionRelacionada(OrdenExamenResource.class, "idConsultaProcedimientoPaso", Set.of("POST")));
    }

    @Override
    protected ConsultaProcedimientoPasoService getService() {
        return service;
    }

    @Override
    protected ConsultaProcedimientoPaso nuevaEntidad(UUID id) {
        return new ConsultaProcedimientoPaso(id);
    }

    @Override
    protected ConsultaProcedimientoPasoDto toDto(ConsultaProcedimientoPaso entidad) {
        ConsultaProcedimientoPasoDto dto = new ConsultaProcedimientoPasoDto();
        dto.setIdConsultaProcedimientoPaso(entidad.getIdConsultaProcedimientoPaso());
        dto.setFechaInicio(entidad.getFechaInicio());
        dto.setFechaFin(entidad.getFechaFin());
        dto.setEstado(entidad.getEstado());
        dto.setValor(entidad.getValor());
        dto.setIdConsultaProcedimiento(entidad.getIdConsultaProcedimiento() == null ? null : entidad.getIdConsultaProcedimiento().getIdConsultaProcedimiento());
        dto.setIdPersonaRol(entidad.getIdPersonaRol() == null ? null : entidad.getIdPersonaRol().getIdPersonaRol());
        dto.setIdProcedimientoPaso(entidad.getIdProcedimientoPaso() == null ? null : entidad.getIdProcedimientoPaso().getIdProcedimientoPaso());
        return dto;
    }

    @Override
    protected void aplicar(ConsultaProcedimientoPasoDto dto, ConsultaProcedimientoPaso entidad) {
        entidad.setFechaInicio(dto.getFechaInicio());
        entidad.setFechaFin(dto.getFechaFin());
        entidad.setEstado(dto.getEstado());
        entidad.setValor(dto.getValor());
        entidad.setIdConsultaProcedimiento(relacion(consultaProcedimientoService, dto.getIdConsultaProcedimiento(), "idConsultaProcedimiento"));
        entidad.setIdPersonaRol(relacion(personaRolService, dto.getIdPersonaRol(), "idPersonaRol"));
        entidad.setIdProcedimientoPaso(relacion(procedimientoPasoService, dto.getIdProcedimientoPaso(), "idProcedimientoPaso"));
    }
}
