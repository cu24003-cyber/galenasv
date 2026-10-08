package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ProcedimientoPasoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.RolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPaso;

@Path("procedimientos-pasos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ProcedimientoPasoResource extends AbstractResource<ProcedimientoPaso, ProcedimientoPasoDto> {

    @Inject
    private ProcedimientoPasoService service;

    @Inject
    private ProcedimientoService procedimientoService;

    @Inject
    private RolService rolService;

    @Override
    protected ProcedimientoPasoService getService() {
        return service;
    }

    @Override
    protected ProcedimientoPaso nuevaEntidad(UUID id) {
        return new ProcedimientoPaso(id);
    }

    @Override
    protected ProcedimientoPasoDto toDto(ProcedimientoPaso entidad) {
        ProcedimientoPasoDto dto = new ProcedimientoPasoDto();
        dto.setIdProcedimientoPaso(entidad.getIdProcedimientoPaso());
        dto.setNombre(entidad.getNombre());
        dto.setIndicaFin(entidad.getIndicaFin());
        dto.setIdProcedimiento(entidad.getIdProcedimiento() == null ? null : entidad.getIdProcedimiento().getIdProcedimiento());
        dto.setIdRol(entidad.getIdRol() == null ? null : entidad.getIdRol().getIdRol());
        return dto;
    }

    @Override
    protected void aplicar(ProcedimientoPasoDto dto, ProcedimientoPaso entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setIndicaFin(dto.getIndicaFin());
        entidad.setIdProcedimiento(relacion(procedimientoService, dto.getIdProcedimiento(), "idProcedimiento"));
        entidad.setIdRol(relacion(rolService, dto.getIdRol(), "idRol"));
    }
}
