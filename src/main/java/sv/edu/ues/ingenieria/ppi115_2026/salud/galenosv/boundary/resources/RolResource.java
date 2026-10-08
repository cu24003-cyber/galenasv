package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.RolDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.RolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

@Path("roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class RolResource extends AbstractResource<Rol, RolDto> {

    @Inject
    private RolService service;

    @Override
    protected RolService getService() {
        return service;
    }

    @Override
    protected Rol nuevaEntidad(UUID id) {
        return new Rol(id);
    }

    @Override
    protected RolDto toDto(Rol entidad) {
        RolDto dto = new RolDto();
        dto.setIdRol(entidad.getIdRol());
        dto.setNombre(entidad.getNombre());
        dto.setActivo(entidad.getActivo());
        dto.setObservaciones(entidad.getObservaciones());
        return dto;
    }

    @Override
    protected void aplicar(RolDto dto, Rol entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setActivo(dto.getActivo());
        entidad.setObservaciones(dto.getObservaciones());
    }
}
