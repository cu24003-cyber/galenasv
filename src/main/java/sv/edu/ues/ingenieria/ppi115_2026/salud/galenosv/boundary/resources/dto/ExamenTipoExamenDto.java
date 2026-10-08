package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Contrato JSON de ExamenTipoExamen; las relaciones se representan mediante UUID. */
public class ExamenTipoExamenDto implements IdentifiableDto {

    private UUID idExamenTipoExamen;

    private OffsetDateTime fechaCreacion;

    @Size(max = 255, message = "observaciones admite hasta 255 caracteres.")
    private String observaciones;

    @NotNull(message = "idExamen es obligatorio.")
    private UUID idExamen;

    @NotNull(message = "idTipoExamen es obligatorio.")
    private UUID idTipoExamen;

    public UUID getIdExamenTipoExamen() {
        return idExamenTipoExamen;
    }

    public void setIdExamenTipoExamen(UUID idExamenTipoExamen) {
        this.idExamenTipoExamen = idExamenTipoExamen;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
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

    public UUID getIdTipoExamen() {
        return idTipoExamen;
    }

    public void setIdTipoExamen(UUID idTipoExamen) {
        this.idTipoExamen = idTipoExamen;
    }

    @Override
    public UUID id() {
        return idExamenTipoExamen;
    }
}
