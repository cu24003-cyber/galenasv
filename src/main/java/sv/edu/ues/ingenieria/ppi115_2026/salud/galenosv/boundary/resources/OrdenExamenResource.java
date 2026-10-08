package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.OrdenExamenDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.OrdenExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.OrdenExamen;

@Path("ordenes-examen")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class OrdenExamenResource extends AbstractResource<OrdenExamen, OrdenExamenDto> {

    @Inject
    private OrdenExamenService service;

    @Inject
    private ConsultaProcedimientoPasoService consultaProcedimientoPasoService;

    @Override
    protected OrdenExamenService getService() {
        return service;
    }

    @Override
    protected OrdenExamen nuevaEntidad(UUID id) {
        return new OrdenExamen(id);
    }

    @Override
    protected OrdenExamenDto toDto(OrdenExamen entidad) {
        OrdenExamenDto dto = new OrdenExamenDto();
        dto.setIdOrdenExamen(entidad.getIdOrdenExamen());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setIndicaciones(entidad.getIndicaciones());
        dto.setIdConsultaProcedimientoPaso(entidad.getIdConsultaProcedimientoPaso() == null ? null : entidad.getIdConsultaProcedimientoPaso().getIdConsultaProcedimientoPaso());
        return dto;
    }

    @Override
    protected void aplicar(OrdenExamenDto dto, OrdenExamen entidad) {
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setIndicaciones(dto.getIndicaciones());
        entidad.setIdConsultaProcedimientoPaso(relacion(consultaProcedimientoPasoService, dto.getIdConsultaProcedimientoPaso(), "idConsultaProcedimientoPaso"));
    }
}
