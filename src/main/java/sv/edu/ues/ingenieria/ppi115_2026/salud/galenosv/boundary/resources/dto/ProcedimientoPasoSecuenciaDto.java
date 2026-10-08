package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Contrato JSON de ProcedimientoPasoSecuencia; las relaciones se representan mediante UUID. */
public class ProcedimientoPasoSecuenciaDto implements IdentifiableDto {

    private UUID idProcedimientoPasoSecuencia;

    private UUID idProcedimientoPasoReferencia;

    @NotBlank(message = "tipoSecuencia es obligatorio.")
    @Size(max = 20, message = "tipoSecuencia admite hasta 20 caracteres.")
    private String tipoSecuencia;

    @NotNull(message = "idProcedimientoPaso es obligatorio.")
    private UUID idProcedimientoPaso;

    public UUID getIdProcedimientoPasoSecuencia() {
        return idProcedimientoPasoSecuencia;
    }

    public void setIdProcedimientoPasoSecuencia(UUID idProcedimientoPasoSecuencia) {
        this.idProcedimientoPasoSecuencia = idProcedimientoPasoSecuencia;
    }

    public UUID getIdProcedimientoPasoReferencia() {
        return idProcedimientoPasoReferencia;
    }

    public void setIdProcedimientoPasoReferencia(UUID idProcedimientoPasoReferencia) {
        this.idProcedimientoPasoReferencia = idProcedimientoPasoReferencia;
    }

    public String getTipoSecuencia() {
        return tipoSecuencia;
    }

    public void setTipoSecuencia(String tipoSecuencia) {
        this.tipoSecuencia = tipoSecuencia;
    }

    public UUID getIdProcedimientoPaso() {
        return idProcedimientoPaso;
    }

    public void setIdProcedimientoPaso(UUID idProcedimientoPaso) {
        this.idProcedimientoPaso = idProcedimientoPaso;
    }

    @Override
    public UUID id() {
        return idProcedimientoPasoSecuencia;
    }
}
