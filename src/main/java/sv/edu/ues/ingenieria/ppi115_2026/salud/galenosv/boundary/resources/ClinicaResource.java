package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ClinicaDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ClinicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Clinica;

@Path("clinicas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ClinicaResource extends AbstractResource<Clinica, ClinicaDto> {

    @Inject
    private ClinicaService service;

    @Override
    protected ClinicaService getService() {
        return service;
    }

    @Override
    protected Clinica nuevaEntidad(UUID id) {
        return new Clinica(id);
    }

    @Override
    protected ClinicaDto toDto(Clinica entidad) {
        ClinicaDto dto = new ClinicaDto();
        dto.setIdClinica(entidad.getIdClinica());
        dto.setNombre(entidad.getNombre());
        dto.setActivo(entidad.getActivo());
        dto.setTipo(entidad.getTipo());
        dto.setComentarios(entidad.getComentarios());
        return dto;
    }

    @Override
    protected void aplicar(ClinicaDto dto, Clinica entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setActivo(dto.getActivo());
        entidad.setTipo(dto.getTipo());
        entidad.setComentarios(dto.getComentarios());
    }
}
