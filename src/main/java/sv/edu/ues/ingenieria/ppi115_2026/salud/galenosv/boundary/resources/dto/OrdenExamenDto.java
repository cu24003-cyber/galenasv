package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Contrato JSON de OrdenExamen; las relaciones se representan mediante UUID. */
public class OrdenExamenDto implements IdentifiableDto {

    private UUID idOrdenExamen;

    private OffsetDateTime fechaCreacion;

    @NotBlank(message = "indicaciones es obligatorio.")
    @Size(max = 255, message = "indicaciones admite hasta 255 caracteres.")
    private String indicaciones;

    @NotNull(message = "idConsultaProcedimientoPaso es obligatorio.")
    private UUID idConsultaProcedimientoPaso;

    public UUID getIdOrdenExamen() {
        return idOrdenExamen;
    }

    public void setIdOrdenExamen(UUID idOrdenExamen) {
        this.idOrdenExamen = idOrdenExamen;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }

    public UUID getIdConsultaProcedimientoPaso() {
        return idConsultaProcedimientoPaso;
    }

    public void setIdConsultaProcedimientoPaso(UUID idConsultaProcedimientoPaso) {
        this.idConsultaProcedimientoPaso = idConsultaProcedimientoPaso;
    }

    @Override
    public UUID id() {
        return idOrdenExamen;
    }
}
