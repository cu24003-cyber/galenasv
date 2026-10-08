package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.PersonaDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

@Path("personas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class PersonaResource extends AbstractResource<Persona, PersonaDto> {

    @Inject
    private PersonaService service;

    @Override
    protected PersonaService getService() {
        return service;
    }

    @Override
    protected Persona nuevaEntidad(UUID id) {
        return new Persona(id);
    }

    @Override
    protected PersonaDto toDto(Persona entidad) {
        PersonaDto dto = new PersonaDto();
        dto.setIdPersona(entidad.getIdPersona());
        dto.setNombres(entidad.getNombres());
        dto.setApellidos(entidad.getApellidos());
        dto.setFechaNacimiento(entidad.getFechaNacimiento());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        return dto;
    }

    @Override
    protected void aplicar(PersonaDto dto, Persona entidad) {
        entidad.setNombres(dto.getNombres());
        entidad.setApellidos(dto.getApellidos());
        entidad.setFechaNacimiento(dto.getFechaNacimiento());
        entidad.setFechaCreacion(dto.getFechaCreacion());
    }
}
