package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ExamenResultadoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenResultadoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.OrdenExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ExamenResultado;

@Path("examenes-resultados")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ExamenResultadoResource extends AbstractResource<ExamenResultado, ExamenResultadoDto> {

    @Inject
    private ExamenResultadoService service;

    @Inject private AtencionService atencion;

    @Override
    protected Set<String> metodosEscritura() { return idPadre() == null ? Set.of("POST", "PUT", "DELETE") : Set.of("POST"); }

    @Override
    protected ExamenResultado crearEntidad(UUID id, ExamenResultadoDto datos) {
        if (idPadre() == null) return super.crearEntidad(id, datos);
        var orden = relacion(ordenExamenService, datos.getIdOrdenExamen(), "idOrdenExamen");
        if (datos.getFechaCreacion() != null) {
            throw new ApiException(
                    Response.Status.BAD_REQUEST, "campos.servidor", "La fecha del resultado la genera el servidor.");
        }
        return atencion.registrarResultado(orden.getIdConsultaProcedimientoPaso().getIdConsultaProcedimiento()
                .getIdConsulta().getIdConsulta(), orden.getIdOrdenExamen(), datos.getResultado(), datos.getInterpretacion());
    }

    @Inject
    private OrdenExamenService ordenExamenService;

    @Override
    protected Map<String, String> filtrosPermitidos() {
        return Map.of(
                "idOrdenExamen", "idOrdenExamen.idOrdenExamen");
    }

    @Override
    protected ExamenResultadoService getService() {
        return service;
    }

    @Override
    protected ExamenResultado nuevaEntidad(UUID id) {
        return new ExamenResultado(id);
    }

    @Override
    protected ExamenResultadoDto toDto(ExamenResultado entidad) {
        ExamenResultadoDto dto = new ExamenResultadoDto();
        dto.setIdExamenResultado(entidad.getIdExamenResultado());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setResultado(entidad.getResultado());
        dto.setInterpretacion(entidad.getInterpretacion());
        dto.setRutaAtestado(entidad.getRutaAtestado());
        dto.setIdOrdenExamen(entidad.getIdOrdenExamen() == null ? null : entidad.getIdOrdenExamen().getIdOrdenExamen());
        return dto;
    }

    @Override
    protected void aplicar(ExamenResultadoDto dto, ExamenResultado entidad) {
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setResultado(dto.getResultado());
        entidad.setInterpretacion(dto.getInterpretacion());
        entidad.setRutaAtestado(dto.getRutaAtestado());
        entidad.setIdOrdenExamen(relacion(ordenExamenService, dto.getIdOrdenExamen(), "idOrdenExamen"));
    }
}
