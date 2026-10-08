package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Contrato JSON de ConsultaProcedimientoPaso; las relaciones se representan mediante UUID. */
public class ConsultaProcedimientoPasoDto implements IdentifiableDto {

    private UUID idConsultaProcedimientoPaso;

    private OffsetDateTime fechaInicio;

    private OffsetDateTime fechaFin;

    @Size(max = 20, message = "estado admite hasta 20 caracteres.")
    private String estado;

    @Size(max = 255, message = "valor admite hasta 255 caracteres.")
    private String valor;

    @NotNull(message = "idConsultaProcedimiento es obligatorio.")
    private UUID idConsultaProcedimiento;

    @NotNull(message = "idPersonaRol es obligatorio.")
    private UUID idPersonaRol;

    @NotNull(message = "idProcedimientoPaso es obligatorio.")
    private UUID idProcedimientoPaso;

    public UUID getIdConsultaProcedimientoPaso() {
        return idConsultaProcedimientoPaso;
    }

    public void setIdConsultaProcedimientoPaso(UUID idConsultaProcedimientoPaso) {
        this.idConsultaProcedimientoPaso = idConsultaProcedimientoPaso;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public UUID getIdConsultaProcedimiento() {
        return idConsultaProcedimiento;
    }

    public void setIdConsultaProcedimiento(UUID idConsultaProcedimiento) {
        this.idConsultaProcedimiento = idConsultaProcedimiento;
    }

    public UUID getIdPersonaRol() {
        return idPersonaRol;
    }

    public void setIdPersonaRol(UUID idPersonaRol) {
        this.idPersonaRol = idPersonaRol;
    }

    public UUID getIdProcedimientoPaso() {
        return idProcedimientoPaso;
    }

    public void setIdProcedimientoPaso(UUID idProcedimientoPaso) {
        this.idProcedimientoPaso = idProcedimientoPaso;
    }

    @Override
    public UUID id() {
        return idConsultaProcedimientoPaso;
    }

    @AssertTrue(message = "fechaFin debe ser posterior o igual a fechaInicio.")
    @JsonbTransient
    public boolean isRangoFechasValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
