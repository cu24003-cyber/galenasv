package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ConsultaProcedimientoPasoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimientoPaso;

@Path("consultas-procedimientos-pasos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ConsultaProcedimientoPasoResource extends AbstractResource<ConsultaProcedimientoPaso, ConsultaProcedimientoPasoDto> {

    @Inject
    private ConsultaProcedimientoPasoService service;

    @Inject
    private ConsultaProcedimientoService consultaProcedimientoService;

    @Inject
    private PersonaRolService personaRolService;

    @Inject
    private ProcedimientoPasoService procedimientoPasoService;

    @Override
    protected ConsultaProcedimientoPasoService getService() {
        return service;
    }

    @Override
    protected ConsultaProcedimientoPaso nuevaEntidad(UUID id) {
        return new ConsultaProcedimientoPaso(id);
    }

    @Override
    protected ConsultaProcedimientoPasoDto toDto(ConsultaProcedimientoPaso entidad) {
        ConsultaProcedimientoPasoDto dto = new ConsultaProcedimientoPasoDto();
        dto.setIdConsultaProcedimientoPaso(entidad.getIdConsultaProcedimientoPaso());
        dto.setFechaInicio(entidad.getFechaInicio());
        dto.setFechaFin(entidad.getFechaFin());
        dto.setEstado(entidad.getEstado());
        dto.setValor(entidad.getValor());
        dto.setIdConsultaProcedimiento(entidad.getIdConsultaProcedimiento() == null ? null : entidad.getIdConsultaProcedimiento().getIdConsultaProcedimiento());
        dto.setIdPersonaRol(entidad.getIdPersonaRol() == null ? null : entidad.getIdPersonaRol().getIdPersonaRol());
        dto.setIdProcedimientoPaso(entidad.getIdProcedimientoPaso() == null ? null : entidad.getIdProcedimientoPaso().getIdProcedimientoPaso());
        return dto;
    }

    @Override
    protected void aplicar(ConsultaProcedimientoPasoDto dto, ConsultaProcedimientoPaso entidad) {
        entidad.setFechaInicio(dto.getFechaInicio());
        entidad.setFechaFin(dto.getFechaFin());
        entidad.setEstado(dto.getEstado());
        entidad.setValor(dto.getValor());
        entidad.setIdConsultaProcedimiento(relacion(consultaProcedimientoService, dto.getIdConsultaProcedimiento(), "idConsultaProcedimiento"));
        entidad.setIdPersonaRol(relacion(personaRolService, dto.getIdPersonaRol(), "idPersonaRol"));
        entidad.setIdProcedimientoPaso(relacion(procedimientoPasoService, dto.getIdProcedimientoPaso(), "idProcedimientoPaso"));
    }
}
