package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.PersonaRolDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ClinicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.RolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

@Path("personas-roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class PersonaRolResource extends AbstractResource<PersonaRol, PersonaRolDto> {

    @Inject
    private PersonaRolService service;

    @Inject
    private ClinicaService clinicaService;

    @Inject
    private PersonaService personaService;

    @Inject
    private RolService rolService;

    @Override
    protected PersonaRolService getService() {
        return service;
    }

    @Override
    protected PersonaRol nuevaEntidad(UUID id) {
        return new PersonaRol(id);
    }

    @Override
    protected PersonaRolDto toDto(PersonaRol entidad) {
        PersonaRolDto dto = new PersonaRolDto();
        dto.setIdPersonaRol(entidad.getIdPersonaRol());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setIdClinica(entidad.getIdClinica() == null ? null : entidad.getIdClinica().getIdClinica());
        dto.setIdPersona(entidad.getIdPersona() == null ? null : entidad.getIdPersona().getIdPersona());
        dto.setIdRol(entidad.getIdRol() == null ? null : entidad.getIdRol().getIdRol());
        return dto;
    }

    @Override
    protected void aplicar(PersonaRolDto dto, PersonaRol entidad) {
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setIdClinica(relacion(clinicaService, dto.getIdClinica(), "idClinica"));
        entidad.setIdPersona(relacion(personaService, dto.getIdPersona(), "idPersona"));
        entidad.setIdRol(relacion(rolService, dto.getIdRol(), "idRol"));
    }
}
