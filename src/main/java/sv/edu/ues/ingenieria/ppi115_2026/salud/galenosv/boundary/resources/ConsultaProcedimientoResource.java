package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ConsultaProcedimientoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimiento;

@Path("consultas-procedimientos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ConsultaProcedimientoResource extends AbstractResource<ConsultaProcedimiento, ConsultaProcedimientoDto> {

    @Inject
    private ConsultaProcedimientoService service;

    @Inject private AtencionService atencion;
    @Inject private AtencionSesion sesion;

    @Override
    protected Set<String> metodosEscritura() { return idPadre() == null ? Set.of("POST", "PUT", "DELETE") : Set.of("POST"); }

    @Override
    protected ConsultaProcedimiento crearEntidad(UUID id, ConsultaProcedimientoDto datos) {
        if (idPadre() == null) return super.crearEntidad(id, datos);
        if (datos.getFechaInicio() != null || datos.getFechaFin() != null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "campos.servidor", "Las fechas del procedimiento las genera el servidor.");
        }
        return atencion.agregarProcedimiento(datos.getIdConsulta(), datos.getIdProcedimiento(), datos.getObservaciones(),
                sesion.getRolActivo() == null ? null : sesion.getRolActivo().getIdPersonaRol());
    }

    @Inject
    private ConsultaService consultaService;

    @Inject
    private ProcedimientoService procedimientoService;

    @Override
    protected Map<String, String> filtrosPermitidos() {
        return Map.of(
                "idConsulta", "idConsulta.idConsulta",
                "idProcedimiento", "idProcedimiento.idProcedimiento");
    }

    @Override
    protected Map<String, ColeccionRelacionada> coleccionesRelacionadas() {
        return Map.of(
                "pasos", new ColeccionRelacionada(ConsultaProcedimientoPasoResource.class, "idConsultaProcedimiento"));
    }

    @Override
    protected ConsultaProcedimientoService getService() {
        return service;
    }

    @Override
    protected ConsultaProcedimiento nuevaEntidad(UUID id) {
        return new ConsultaProcedimiento(id);
    }

    @Override
    protected ConsultaProcedimientoDto toDto(ConsultaProcedimiento entidad) {
        ConsultaProcedimientoDto dto = new ConsultaProcedimientoDto();
        dto.setIdConsultaProcedimiento(entidad.getIdConsultaProcedimiento());
        dto.setIdProcedimiento(entidad.getIdProcedimiento() == null ? null : entidad.getIdProcedimiento().getIdProcedimiento());
        dto.setFechaInicio(entidad.getFechaInicio());
        dto.setFechaFin(entidad.getFechaFin());
        dto.setObservaciones(entidad.getObservaciones());
        dto.setIdConsulta(entidad.getIdConsulta() == null ? null : entidad.getIdConsulta().getIdConsulta());
        return dto;
    }

    @Override
    protected void aplicar(ConsultaProcedimientoDto dto, ConsultaProcedimiento entidad) {
        entidad.setIdProcedimiento(relacion(procedimientoService, dto.getIdProcedimiento(), "idProcedimiento"));
        entidad.setFechaInicio(dto.getFechaInicio());
        entidad.setFechaFin(dto.getFechaFin());
        entidad.setObservaciones(dto.getObservaciones());
        entidad.setIdConsulta(relacion(consultaService, dto.getIdConsulta(), "idConsulta"));
    }
}
