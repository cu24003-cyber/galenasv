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
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ProcedimientoPasoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConfiguracionProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.RolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPaso;

@Path("procedimientos-pasos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ProcedimientoPasoResource extends AbstractResource<ProcedimientoPaso, ProcedimientoPasoDto> {

    @Inject
    private ProcedimientoPasoService service;

    @Inject private ConfiguracionProcedimientoService configuracion;

    @Override
    protected ProcedimientoPaso crearEntidad(UUID id, ProcedimientoPasoDto datos) {
        if (idPadre() == null) {
            if (datos.getIdPasoPrevio() != null) throw new ApiException(
                    Response.Status.BAD_REQUEST, "ruta.requerida", "Use la ruta del procedimiento para indicar un paso previo.");
            return super.crearEntidad(id, datos);
        }
        return configuracion.paso(datos.getIdProcedimiento(), datos.getNombre(), Boolean.TRUE.equals(datos.getIndicaFin()),
                datos.getIdRol(), datos.getIdPasoPrevio());
    }

    @Override
    protected void actualizarEntidad(ProcedimientoPaso entidad, ProcedimientoPasoDto datos) {
        if (datos.getIdPasoPrevio() != null) throw new ApiException(
                Response.Status.BAD_REQUEST, "campo.soloCreacion", "idPasoPrevio solo se admite al crear un paso.");
        super.actualizarEntidad(entidad, datos);
    }

    @Inject
    private ProcedimientoService procedimientoService;

    @Inject
    private RolService rolService;

    @Override
    protected Map<String, String> filtrosPermitidos() {
        return Map.of(
                "idProcedimiento", "idProcedimiento.idProcedimiento",
                "idRol", "idRol.idRol");
    }

    @Override
    protected Map<String, ColeccionRelacionada> coleccionesRelacionadas() {
        return Map.of(
                "examenes-configurados", new ColeccionRelacionada(ProcedimientoPasoExamenResource.class, "idProcedimientoPaso", Set.of("POST")),
                "secuencias", new ColeccionRelacionada(ProcedimientoPasoSecuenciaResource.class, "idProcedimientoPaso"),
                "pasos-consulta", new ColeccionRelacionada(ConsultaProcedimientoPasoResource.class, "idProcedimientoPaso"));
    }

    @Override
    protected ProcedimientoPasoService getService() {
        return service;
    }

    @Override
    protected ProcedimientoPaso nuevaEntidad(UUID id) {
        return new ProcedimientoPaso(id);
    }

    @Override
    protected ProcedimientoPasoDto toDto(ProcedimientoPaso entidad) {
        ProcedimientoPasoDto dto = new ProcedimientoPasoDto();
        dto.setIdProcedimientoPaso(entidad.getIdProcedimientoPaso());
        dto.setNombre(entidad.getNombre());
        dto.setIndicaFin(entidad.getIndicaFin());
        dto.setIdProcedimiento(entidad.getIdProcedimiento() == null ? null : entidad.getIdProcedimiento().getIdProcedimiento());
        dto.setIdRol(entidad.getIdRol() == null ? null : entidad.getIdRol().getIdRol());
        return dto;
    }

    @Override
    protected void aplicar(ProcedimientoPasoDto dto, ProcedimientoPaso entidad) {
        entidad.setNombre(dto.getNombre());
        entidad.setIndicaFin(dto.getIndicaFin());
        entidad.setIdProcedimiento(relacion(procedimientoService, dto.getIdProcedimiento(), "idProcedimiento"));
        entidad.setIdRol(relacion(rolService, dto.getIdRol(), "idRol"));
    }
}
