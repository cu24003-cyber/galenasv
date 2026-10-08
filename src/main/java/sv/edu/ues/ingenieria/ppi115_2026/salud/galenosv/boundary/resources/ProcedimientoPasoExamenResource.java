package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ProcedimientoPasoExamenDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPasoExamen;

@Path("procedimientos-pasos-examenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ProcedimientoPasoExamenResource extends AbstractResource<ProcedimientoPasoExamen, ProcedimientoPasoExamenDto> {

    @Inject
    private ProcedimientoPasoExamenService service;

    @Inject
    private ExamenService examenService;

    @Inject
    private ProcedimientoPasoService procedimientoPasoService;

    @Override
    protected ProcedimientoPasoExamenService getService() {
        return service;
    }

    @Override
    protected ProcedimientoPasoExamen nuevaEntidad(UUID id) {
        return new ProcedimientoPasoExamen(id);
    }

    @Override
    protected ProcedimientoPasoExamenDto toDto(ProcedimientoPasoExamen entidad) {
        ProcedimientoPasoExamenDto dto = new ProcedimientoPasoExamenDto();
        dto.setIdProcedimientoPasoExamen(entidad.getIdProcedimientoPasoExamen());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setActivo(entidad.getActivo());
        dto.setObservaciones(entidad.getObservaciones());
        dto.setIdExamen(entidad.getIdExamen() == null ? null : entidad.getIdExamen().getIdExamen());
        dto.setIdProcedimientoPaso(entidad.getIdProcedimientoPaso() == null ? null : entidad.getIdProcedimientoPaso().getIdProcedimientoPaso());
        return dto;
    }

    @Override
    protected void aplicar(ProcedimientoPasoExamenDto dto, ProcedimientoPasoExamen entidad) {
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setActivo(dto.getActivo());
        entidad.setObservaciones(dto.getObservaciones());
        entidad.setIdExamen(relacion(examenService, dto.getIdExamen(), "idExamen"));
        entidad.setIdProcedimientoPaso(relacion(procedimientoPasoService, dto.getIdProcedimientoPaso(), "idProcedimientoPaso"));
    }
}
