package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Contrato JSON de Consulta; las relaciones se representan mediante UUID. */
public class ConsultaDto implements IdentifiableDto {

    private UUID idConsulta;

    private OffsetDateTime fechaInicio;

    private OffsetDateTime fechaFin;

    @Size(max = 255, message = "referenciaExterna admite hasta 255 caracteres.")
    private String referenciaExterna;

    @Size(max = 255, message = "observaciones admite hasta 255 caracteres.")
    private String observaciones;

    @NotNull(message = "idPersonaRol es obligatorio.")
    private UUID idPersonaRol;

    public UUID getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(UUID idConsulta) {
        this.idConsulta = idConsulta;
    }

    public OffsetDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(OffsetDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public OffsetDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(OffsetDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public UUID getIdPersonaRol() {
        return idPersonaRol;
    }

    public void setIdPersonaRol(UUID idPersonaRol) {
        this.idPersonaRol = idPersonaRol;
    }

    @Override
    public UUID id() {
        return idConsulta;
    }

    @AssertTrue(message = "fechaFin debe ser posterior o igual a fechaInicio.")
    @JsonbTransient
    public boolean isRangoFechasValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
