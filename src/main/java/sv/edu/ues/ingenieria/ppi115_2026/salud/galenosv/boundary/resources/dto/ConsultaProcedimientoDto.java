package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Contrato JSON de ConsultaProcedimiento; las relaciones se representan mediante UUID. */
public class ConsultaProcedimientoDto implements IdentifiableDto {

    private UUID idConsultaProcedimiento;

    @NotNull(message = "idProcedimiento es obligatorio.")
    private UUID idProcedimiento;

    private OffsetDateTime fechaInicio;

    private OffsetDateTime fechaFin;

    @Size(max = 255, message = "observaciones admite hasta 255 caracteres.")
    private String observaciones;

    @NotNull(message = "idConsulta es obligatorio.")
    private UUID idConsulta;

    public UUID getIdConsultaProcedimiento() {
        return idConsultaProcedimiento;
    }

    public void setIdConsultaProcedimiento(UUID idConsultaProcedimiento) {
        this.idConsultaProcedimiento = idConsultaProcedimiento;
    }

    public UUID getIdProcedimiento() {
        return idProcedimiento;
    }

    public void setIdProcedimiento(UUID idProcedimiento) {
        this.idProcedimiento = idProcedimiento;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public UUID getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(UUID idConsulta) {
        this.idConsulta = idConsulta;
    }

    @Override
    public UUID id() {
        return idConsultaProcedimiento;
    }

    @AssertTrue(message = "fechaFin debe ser posterior o igual a fechaInicio.")
    @JsonbTransient
    public boolean isRangoFechasValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
