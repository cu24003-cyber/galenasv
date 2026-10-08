package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ProcedimientoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Procedimiento;

@Path("procedimientos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ProcedimientoResource extends AbstractResource<Procedimiento, ProcedimientoDto> {

    @Inject
    private ProcedimientoService service;

    @Override
    protected ProcedimientoService getService() {
        return service;
    }

    @Override
    protected Procedimiento nuevaEntidad(UUID id) {
        return new Procedimiento(id);
    }

    @Override
    protected ProcedimientoDto toDto(Procedimiento entidad) {
        ProcedimientoDto dto = new ProcedimientoDto();
        dto.setIdProcedimiento(entidad.getIdProcedimiento());
        dto.setNombre(entidad.getNombre());
        dto.setActivo(entidad.getActivo());
        dto.setObservaciones(entidad.getObservaciones());
        return dto;
    }

    @Override
    protected void aplicar(ProcedimientoDto dto, Procedimiento entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setActivo(dto.getActivo());
        entidad.setObservaciones(dto.getObservaciones());
    }
}
