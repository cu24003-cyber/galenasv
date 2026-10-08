package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ProcedimientoPasoSecuenciaDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoSecuenciaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPasoSecuencia;

@Path("procedimientos-pasos-secuencias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ProcedimientoPasoSecuenciaResource extends AbstractResource<ProcedimientoPasoSecuencia, ProcedimientoPasoSecuenciaDto> {

    @Inject
    private ProcedimientoPasoSecuenciaService service;

    @Inject
    private ProcedimientoPasoService procedimientoPasoService;

    @Override
    protected ProcedimientoPasoSecuenciaService getService() {
        return service;
    }

    @Override
    protected ProcedimientoPasoSecuencia nuevaEntidad(UUID id) {
        return new ProcedimientoPasoSecuencia(id);
    }

    @Override
    protected ProcedimientoPasoSecuenciaDto toDto(ProcedimientoPasoSecuencia entidad) {
        ProcedimientoPasoSecuenciaDto dto = new ProcedimientoPasoSecuenciaDto();
        dto.setIdProcedimientoPasoSecuencia(entidad.getIdProcedimientoPasoSecuencia());
        dto.setIdProcedimientoPasoReferencia(entidad.getIdProcedimientoPasoReferencia() == null ? null : entidad.getIdProcedimientoPasoReferencia().getIdProcedimientoPaso());
        dto.setTipoSecuencia(entidad.getTipoSecuencia());
        dto.setIdProcedimientoPaso(entidad.getIdProcedimientoPaso() == null ? null : entidad.getIdProcedimientoPaso().getIdProcedimientoPaso());
        return dto;
    }

    @Override
    protected void aplicar(ProcedimientoPasoSecuenciaDto dto, ProcedimientoPasoSecuencia entidad) {
        entidad.setIdProcedimientoPasoReferencia(relacion(procedimientoPasoService, dto.getIdProcedimientoPasoReferencia(), "idProcedimientoPasoReferencia"));
        entidad.setTipoSecuencia(dto.getTipoSecuencia());
        entidad.setIdProcedimientoPaso(relacion(procedimientoPasoService, dto.getIdProcedimientoPaso(), "idProcedimientoPaso"));
    }
}
