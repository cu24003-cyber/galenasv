package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ConsultaProcedimientoDto;
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

    @Inject
    private ConsultaService consultaService;

    @Inject
    private ProcedimientoService procedimientoService;

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
