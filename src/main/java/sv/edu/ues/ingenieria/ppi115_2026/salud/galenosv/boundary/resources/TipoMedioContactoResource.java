package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.TipoMedioContactoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoMedioContactoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoMedioContacto;

@Path("tipos-medio-contacto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class TipoMedioContactoResource extends AbstractResource<TipoMedioContacto, TipoMedioContactoDto> {

    @Inject
    private TipoMedioContactoService service;

    @Override
    protected TipoMedioContactoService getService() {
        return service;
    }

    @Override
    protected TipoMedioContacto nuevaEntidad(UUID id) {
        return new TipoMedioContacto(id);
    }

    @Override
    protected TipoMedioContactoDto toDto(TipoMedioContacto entidad) {
        TipoMedioContactoDto dto = new TipoMedioContactoDto();
        dto.setIdTipoMedioContacto(entidad.getIdTipoMedioContacto());
        dto.setNombre(entidad.getNombre());
        dto.setIndicaciones(entidad.getIndicaciones());
        dto.setExpresionRegular(entidad.getExpresionRegular());
        dto.setActivo(entidad.getActivo());
        return dto;
    }

    @Override
    protected void aplicar(TipoMedioContactoDto dto, TipoMedioContacto entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setIndicaciones(dto.getIndicaciones());
        entidad.setExpresionRegular(dto.getExpresionRegular());
        entidad.setActivo(dto.getActivo());
    }
}
