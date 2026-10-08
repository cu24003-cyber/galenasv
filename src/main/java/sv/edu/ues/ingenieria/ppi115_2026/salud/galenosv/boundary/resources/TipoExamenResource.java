package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.TipoExamenDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoExamen;

@Path("tipos-examen")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class TipoExamenResource extends AbstractResource<TipoExamen, TipoExamenDto> {

    @Inject
    private TipoExamenService service;

    @Override
    protected TipoExamenService getService() {
        return service;
    }

    @Override
    protected TipoExamen nuevaEntidad(UUID id) {
        return new TipoExamen(id);
    }

    @Override
    protected TipoExamenDto toDto(TipoExamen entidad) {
        TipoExamenDto dto = new TipoExamenDto();
        dto.setIdTipoExamen(entidad.getIdTipoExamen());
        dto.setNombre(entidad.getNombre());
        dto.setActivo(entidad.getActivo());
        dto.setObservaciones(entidad.getObservaciones());
        return dto;
    }

    @Override
    protected void aplicar(TipoExamenDto dto, TipoExamen entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setActivo(dto.getActivo());
        entidad.setObservaciones(dto.getObservaciones());
    }
}
