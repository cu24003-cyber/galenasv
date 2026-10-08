package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.MedioContactoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.MedioContactoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoMedioContactoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.MedioContacto;

@Path("medios-contacto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class MedioContactoResource extends AbstractResource<MedioContacto, MedioContactoDto> {

    @Inject
    private MedioContactoService service;

    @Inject
    private PersonaService personaService;

    @Inject
    private TipoMedioContactoService tipoMedioContactoService;

    @Override
    protected MedioContactoService getService() {
        return service;
    }

    @Override
    protected MedioContacto nuevaEntidad(UUID id) {
        return new MedioContacto(id);
    }

    @Override
    protected MedioContactoDto toDto(MedioContacto entidad) {
        MedioContactoDto dto = new MedioContactoDto();
        dto.setIdMedioContacto(entidad.getIdMedioContacto());
        dto.setValor(entidad.getValor());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setIdPersona(entidad.getIdPersona() == null ? null : entidad.getIdPersona().getIdPersona());
        dto.setIdTipoMedioContacto(entidad.getIdTipoMedioContacto() == null ? null : entidad.getIdTipoMedioContacto().getIdTipoMedioContacto());
        return dto;
    }

    @Override
    protected void aplicar(MedioContactoDto dto, MedioContacto entidad) {
        entidad.setValor(dto.getValor());
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setIdPersona(relacion(personaService, dto.getIdPersona(), "idPersona"));
        entidad.setIdTipoMedioContacto(relacion(tipoMedioContactoService, dto.getIdTipoMedioContacto(), "idTipoMedioContacto"));
    }
}
