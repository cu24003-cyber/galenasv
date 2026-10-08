package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Contrato JSON de Documento; las relaciones se representan mediante UUID. */
public class DocumentoDto implements IdentifiableDto {

    private UUID idDocumento;

    @NotBlank(message = "valor es obligatorio.")
    @Size(max = 255, message = "valor admite hasta 255 caracteres.")
    private String valor;

    @Size(max = 255, message = "rutaFisica admite hasta 255 caracteres.")
    private String rutaFisica;

    @NotNull(message = "idPersona es obligatorio.")
    private UUID idPersona;

    @NotNull(message = "idTipoDocumento es obligatorio.")
    private UUID idTipoDocumento;

    public UUID getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(UUID idDocumento) {
        this.idDocumento = idDocumento;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getRutaFisica() {
        return rutaFisica;
    }

    public void setRutaFisica(String rutaFisica) {
        this.rutaFisica = rutaFisica;
    }

    public UUID getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(UUID idPersona) {
        this.idPersona = idPersona;
    }

    public UUID getIdTipoDocumento() {
        return idTipoDocumento;
    }

    public void setIdTipoDocumento(UUID idTipoDocumento) {
        this.idTipoDocumento = idTipoDocumento;
    }

    @Override
    public UUID id() {
        return idDocumento;
    }
}
