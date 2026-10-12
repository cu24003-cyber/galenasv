package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.AtencionDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ConsultaDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Consulta;

@Path("consultas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ConsultaResource extends AbstractResource<Consulta, ConsultaDto> {

    @Inject
    private ConsultaService service;

    @Inject private AtencionService atencion;
    @Inject private AtencionSesion sesion;

    @Override
    protected Set<String> metodosEscritura() { return idPadre() == null ? CRUD : Set.of("POST", "PUT", "PATCH"); }

    @Override
    protected Set<String> camposInmutables() { return Set.of("idPersonaRol", "fechaInicio", "fechaFin"); }

    @Override
    protected Consulta crearEntidad(UUID id, ConsultaDto datos) {
        if (idPadre() == null) return super.crearEntidad(id, datos);
        if (datos.getFechaInicio() != null || datos.getFechaFin() != null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "campos.servidor", "Las fechas de la consulta las genera el servidor.");
        }
        Consulta consulta = atencion.crear(datos.getIdPersonaRol(), datos.getReferenciaExterna(), datos.getObservaciones());
        sesion.setConsulta(consulta.getIdConsulta());
        return consulta;
    }

    @Override
    protected void actualizarEntidad(Consulta entidad, ConsultaDto datos) {
        if (idPadre() == null && !esModificacionParcial()) {
            super.actualizarEntidad(entidad, datos);
            return;
        }
        if (!entidad.getIdPersonaRol().getIdPersonaRol().equals(datos.getIdPersonaRol())
                || (datos.getFechaInicio() != null && !datos.getFechaInicio().equals(entidad.getFechaInicio()))
                || (datos.getFechaFin() != null && !datos.getFechaFin().equals(entidad.getFechaFin()))) {
            throw new ApiException(Response.Status.BAD_REQUEST, "campos.servidor", "El paciente y las fechas no se modifican; use la operación de cierre.");
        }
        atencion.guardar(entidad.getIdConsulta(), datos.getReferenciaExterna(), datos.getObservaciones());
    }

    @PUT
    @Path("{id}/cierre")
    public Response cerrar(@PathParam("id") String id, @Valid @NotNull AtencionDto.Notas datos) {
        Consulta consulta = buscarExistente(parseId(id));
        atencion.cargar(consulta.getIdConsulta());
        if (consulta.getFechaFin() == null) {
            atencion.cerrar(consulta.getIdConsulta(),
                    datos.getReferenciaExterna() == null ? consulta.getReferenciaExterna() : datos.getReferenciaExterna(),
                    datos.getObservaciones() == null ? consulta.getObservaciones() : datos.getObservaciones());
        }
        if (consulta.getIdConsulta().equals(sesion.getConsulta())) sesion.setConsulta(null);
        return Response.ok(toDto(consulta)).build();
    }

    @PUT
    @Path("{id}/sesion")
    public Response retomar(@PathParam("id") String id) {
        Consulta consulta = buscarExistente(parseId(id));
        atencion.cargar(consulta.getIdConsulta());
        if (consulta.getFechaFin() != null) {
            throw new ApiException(Response.Status.CONFLICT, "consulta.yaCerrada", "La consulta ya está cerrada.");
        }
        if (sesion.getConsulta() != null && !consulta.getIdConsulta().equals(sesion.getConsulta())) {
            throw new ApiException(Response.Status.CONFLICT, "consulta.cerrarAntesRetomar", "Cierre la consulta en curso antes de retomar otra.");
        }
        sesion.setConsulta(consulta.getIdConsulta());
        return Response.ok(toDto(consulta)).build();
    }

    @Inject
    private PersonaRolService personaRolService;

    @Override
    protected Map<String, String> filtrosPermitidos() {
        return Map.of(
                "idPersonaRol", "idPersonaRol.idPersonaRol",
                "idPersona", "idPersonaRol.idPersona.idPersona",
                "idClinica", "idPersonaRol.idClinica.idClinica",
                "idRol", "idPersonaRol.idRol.idRol");
    }

    @Override
    protected Map<String, ColeccionRelacionada> coleccionesRelacionadas() {
        return Map.of(
                "procedimientos", new ColeccionRelacionada(ConsultaProcedimientoResource.class, "idConsulta", Set.of("POST")));
    }

    @Override
    protected ConsultaService getService() {
        return service;
    }

    @Override
    protected Consulta nuevaEntidad(UUID id) {
        return new Consulta(id);
    }

    @Override
    protected ConsultaDto toDto(Consulta entidad) {
        ConsultaDto dto = new ConsultaDto();
        dto.setIdConsulta(entidad.getIdConsulta());
        dto.setFechaInicio(entidad.getFechaInicio());
        dto.setFechaFin(entidad.getFechaFin());
        dto.setReferenciaExterna(entidad.getReferenciaExterna());
        dto.setObservaciones(entidad.getObservaciones());
        dto.setIdPersonaRol(entidad.getIdPersonaRol() == null ? null : entidad.getIdPersonaRol().getIdPersonaRol());
        return dto;
    }

    @Override
    protected void aplicar(ConsultaDto dto, Consulta entidad) {
        entidad.setFechaInicio(dto.getFechaInicio());
        entidad.setFechaFin(dto.getFechaFin());
        entidad.setReferenciaExterna(dto.getReferenciaExterna());
        entidad.setObservaciones(dto.getObservaciones());
        entidad.setIdPersonaRol(relacion(personaRolService, dto.getIdPersonaRol(), "idPersonaRol"));
    }
}
