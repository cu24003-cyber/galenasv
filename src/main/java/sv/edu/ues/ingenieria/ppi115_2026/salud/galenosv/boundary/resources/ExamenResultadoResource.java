package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ExamenResultadoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenResultadoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.OrdenExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ExamenResultado;

@Path("examenes-resultados")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ExamenResultadoResource extends AbstractResource<ExamenResultado, ExamenResultadoDto> {

    @Inject
    private ExamenResultadoService service;

    @Inject
    private OrdenExamenService ordenExamenService;

    @Override
    protected ExamenResultadoService getService() {
        return service;
    }

    @Override
    protected ExamenResultado nuevaEntidad(UUID id) {
        return new ExamenResultado(id);
    }

    @Override
    protected ExamenResultadoDto toDto(ExamenResultado entidad) {
        ExamenResultadoDto dto = new ExamenResultadoDto();
        dto.setIdExamenResultado(entidad.getIdExamenResultado());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setResultado(entidad.getResultado());
        dto.setInterpretacion(entidad.getInterpretacion());
        dto.setRutaAtestado(entidad.getRutaAtestado());
        dto.setIdOrdenExamen(entidad.getIdOrdenExamen() == null ? null : entidad.getIdOrdenExamen().getIdOrdenExamen());
        return dto;
    }

    @Override
    protected void aplicar(ExamenResultadoDto dto, ExamenResultado entidad) {
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setResultado(dto.getResultado());
        entidad.setInterpretacion(dto.getInterpretacion());
        entidad.setRutaAtestado(dto.getRutaAtestado());
        entidad.setIdOrdenExamen(relacion(ordenExamenService, dto.getIdOrdenExamen(), "idOrdenExamen"));
    }
}
