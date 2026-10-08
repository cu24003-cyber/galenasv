package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ExamenTipoExamenDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenTipoExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ExamenTipoExamen;

@Path("examenes-tipos-examen")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ExamenTipoExamenResource extends AbstractResource<ExamenTipoExamen, ExamenTipoExamenDto> {

    @Inject
    private ExamenTipoExamenService service;

    @Inject
    private ExamenService examenService;

    @Inject
    private TipoExamenService tipoExamenService;

    @Override
    protected ExamenTipoExamenService getService() {
        return service;
    }

    @Override
    protected ExamenTipoExamen nuevaEntidad(UUID id) {
        return new ExamenTipoExamen(id);
    }

    @Override
    protected ExamenTipoExamenDto toDto(ExamenTipoExamen entidad) {
        ExamenTipoExamenDto dto = new ExamenTipoExamenDto();
        dto.setIdExamenTipoExamen(entidad.getIdExamenTipoExamen());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setObservaciones(entidad.getObservaciones());
        dto.setIdExamen(entidad.getIdExamen() == null ? null : entidad.getIdExamen().getIdExamen());
        dto.setIdTipoExamen(entidad.getIdTipoExamen() == null ? null : entidad.getIdTipoExamen().getIdTipoExamen());
        return dto;
    }

    @Override
    protected void aplicar(ExamenTipoExamenDto dto, ExamenTipoExamen entidad) {
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setObservaciones(dto.getObservaciones());
        entidad.setIdExamen(relacion(examenService, dto.getIdExamen(), "idExamen"));
        entidad.setIdTipoExamen(relacion(tipoExamenService, dto.getIdTipoExamen(), "idTipoExamen"));
    }
}
