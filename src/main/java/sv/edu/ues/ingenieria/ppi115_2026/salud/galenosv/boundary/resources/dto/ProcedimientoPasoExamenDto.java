package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Contrato JSON de ProcedimientoPasoExamen; las relaciones se representan mediante UUID. */
public class ProcedimientoPasoExamenDto implements IdentifiableDto {

    private UUID idProcedimientoPasoExamen;

    private OffsetDateTime fechaCreacion;

    private Boolean activo;

    @Size(max = 255, message = "observaciones admite hasta 255 caracteres.")
    private String observaciones;

    @NotNull(message = "idExamen es obligatorio.")
    private UUID idExamen;

    @NotNull(message = "idProcedimientoPaso es obligatorio.")
    private UUID idProcedimientoPaso;

    public UUID getIdProcedimientoPasoExamen() {
        return idProcedimientoPasoExamen;
    }

    public void setIdProcedimientoPasoExamen(UUID idProcedimientoPasoExamen) {
        this.idProcedimientoPasoExamen = idProcedimientoPasoExamen;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public UUID getIdExamen() {
        return idExamen;
    }

    public void setIdExamen(UUID idExamen) {
        this.idExamen = idExamen;
    }

    public UUID getIdProcedimientoPaso() {
        return idProcedimientoPaso;
    }

    public void setIdProcedimientoPaso(UUID idProcedimientoPaso) {
        this.idProcedimientoPaso = idProcedimientoPaso;
    }

    @Override
    public UUID id() {
        return idProcedimientoPasoExamen;
    }
}
