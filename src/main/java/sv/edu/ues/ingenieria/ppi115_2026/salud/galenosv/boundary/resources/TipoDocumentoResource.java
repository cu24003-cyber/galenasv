package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.TipoDocumentoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

@Path("tipos-documento")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class TipoDocumentoResource extends AbstractResource<TipoDocumento, TipoDocumentoDto> {

    @Inject
    private TipoDocumentoService service;

    @Override
    protected TipoDocumentoService getService() {
        return service;
    }

    @Override
    protected TipoDocumento nuevaEntidad(UUID id) {
        return new TipoDocumento(id);
    }

    @Override
    protected TipoDocumentoDto toDto(TipoDocumento entidad) {
        TipoDocumentoDto dto = new TipoDocumentoDto();
        dto.setIdTipoDocumento(entidad.getIdTipoDocumento());
        dto.setNombre(entidad.getNombre());
        dto.setIndicaciones(entidad.getIndicaciones());
        dto.setExpresionRegular(entidad.getExpresionRegular());
        dto.setActivo(entidad.getActivo());
        return dto;
    }

    @Override
    protected void aplicar(TipoDocumentoDto dto, TipoDocumento entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setIndicaciones(dto.getIndicaciones());
        entidad.setExpresionRegular(dto.getExpresionRegular());
        entidad.setActivo(dto.getActivo());
    }
}
