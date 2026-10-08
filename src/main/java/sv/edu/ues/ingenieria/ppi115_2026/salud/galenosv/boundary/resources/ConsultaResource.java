package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ConsultaDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Consulta;

@Path("consultas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ConsultaResource extends AbstractResource<Consulta, ConsultaDto> {

    @Inject
    private ConsultaService service;

    @Inject
    private PersonaRolService personaRolService;

    @Override
    protected ConsultaService getService() {
        return service;
    }

    @Override
    protected Consulta nuevaEntidad(UUID id) {
        return new Consulta(id);
    }

    @Override
    protected ConsultaDto toDto(Consulta entidad) {
        ConsultaDto dto = new ConsultaDto();
        dto.setIdConsulta(entidad.getIdConsulta());
        dto.setFechaInicio(entidad.getFechaInicio());
        dto.setFechaFin(entidad.getFechaFin());
        dto.setReferenciaExterna(entidad.getReferenciaExterna());
        dto.setObservaciones(entidad.getObservaciones());
        dto.setIdPersonaRol(entidad.getIdPersonaRol() == null ? null : entidad.getIdPersonaRol().getIdPersonaRol());
        return dto;
    }

    @Override
    protected void aplicar(ConsultaDto dto, Consulta entidad) {
        entidad.setFechaInicio(dto.getFechaInicio());
        entidad.setFechaFin(dto.getFechaFin());
        entidad.setReferenciaExterna(dto.getReferenciaExterna());
        entidad.setObservaciones(dto.getObservaciones());
        entidad.setIdPersonaRol(relacion(personaRolService, dto.getIdPersonaRol(), "idPersonaRol"));
    }
}
