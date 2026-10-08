package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ExamenDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Examen;

@Path("examenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ExamenResource extends AbstractResource<Examen, ExamenDto> {

    @Inject
    private ExamenService service;

    @Override
    protected ExamenService getService() {
        return service;
    }

    @Override
    protected Examen nuevaEntidad(UUID id) {
        return new Examen(id);
    }

    @Override
    protected ExamenDto toDto(Examen entidad) {
        ExamenDto dto = new ExamenDto();
        dto.setIdExamen(entidad.getIdExamen());
        dto.setNombre(entidad.getNombre());
        dto.setActivo(entidad.getActivo());
        dto.setObservaciones(entidad.getObservaciones());
        return dto;
    }

    @Override
    protected void aplicar(ExamenDto dto, Examen entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setActivo(dto.getActivo());
        entidad.setObservaciones(dto.getObservaciones());
    }
}
