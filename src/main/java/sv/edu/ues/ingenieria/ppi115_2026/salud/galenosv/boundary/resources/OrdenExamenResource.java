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
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.OrdenExamenDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.OrdenExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.OrdenExamen;

@Path("ordenes-examen")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class OrdenExamenResource extends AbstractResource<OrdenExamen, OrdenExamenDto> {

    @Inject
    private OrdenExamenService service;

    @Inject private AtencionService atencion;

    @Override
    protected Set<String> metodosEscritura() { return idPadre() == null ? Set.of("POST", "PUT", "DELETE") : Set.of("POST"); }

    @Override
    protected OrdenExamen crearEntidad(UUID id, OrdenExamenDto datos) {
        if (idPadre() == null) return super.crearEntidad(id, datos);
        var paso = relacion(consultaProcedimientoPasoService, datos.getIdConsultaProcedimientoPaso(), "idConsultaProcedimientoPaso");
        if (datos.getFechaCreacion() != null) {
            throw new ApiException(
                    Response.Status.BAD_REQUEST, "campos.servidor", "La fecha de la orden la genera el servidor.");
        }
        return atencion.ordenar(paso.getIdConsultaProcedimiento().getIdConsulta().getIdConsulta(),
                paso.getIdConsultaProcedimientoPaso(), datos.getIdExamen(), datos.getIndicaciones());
    }

    @Inject
    private ConsultaProcedimientoPasoService consultaProcedimientoPasoService;

    @Inject
    private ExamenService examenService;

    @Override
    protected Map<String, String> filtrosPermitidos() {
        return Map.of(
                "idConsultaProcedimientoPaso", "idConsultaProcedimientoPaso.idConsultaProcedimientoPaso");
    }

    @Override
    protected Map<String, ColeccionRelacionada> coleccionesRelacionadas() {
        return Map.of(
                "resultados", new ColeccionRelacionada(ExamenResultadoResource.class, "idOrdenExamen", Set.of("POST")));
    }

    @Override
    protected OrdenExamenService getService() {
        return service;
    }

    @Override
    protected OrdenExamen nuevaEntidad(UUID id) {
        return new OrdenExamen(id);
    }

    @Override
    protected OrdenExamenDto toDto(OrdenExamen entidad) {
        OrdenExamenDto dto = new OrdenExamenDto();
        dto.setIdOrdenExamen(entidad.getIdOrdenExamen());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setIndicaciones(entidad.getIndicaciones());
        dto.setIdConsultaProcedimientoPaso(entidad.getIdConsultaProcedimientoPaso() == null ? null : entidad.getIdConsultaProcedimientoPaso().getIdConsultaProcedimientoPaso());
        dto.setIdExamen(entidad.getIdExamen() == null ? null : entidad.getIdExamen().getIdExamen());
        return dto;
    }

    @Override
    protected void aplicar(OrdenExamenDto dto, OrdenExamen entidad) {
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setIndicaciones(dto.getIndicaciones());
        entidad.setIdConsultaProcedimientoPaso(relacion(consultaProcedimientoPasoService, dto.getIdConsultaProcedimientoPaso(), "idConsultaProcedimientoPaso"));
        entidad.setIdExamen(relacion(examenService, dto.getIdExamen(), "idExamen"));
    }
}
