package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.UUID;

/** Contrato JSON de MedioContacto; las relaciones se representan mediante UUID. */
public class MedioContactoDto implements IdentifiableDto {

    private UUID idMedioContacto;

    @NotBlank(message = "valor es obligatorio.")
    @Size(max = 255, message = "valor admite hasta 255 caracteres.")
    private String valor;

    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date fechaCreacion;

    @NotNull(message = "idPersona es obligatorio.")
    private UUID idPersona;

    @NotNull(message = "idTipoMedioContacto es obligatorio.")
    private UUID idTipoMedioContacto;

    public UUID getIdMedioContacto() {
        return idMedioContacto;
    }

    public void setIdMedioContacto(UUID idMedioContacto) {
        this.idMedioContacto = idMedioContacto;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public UUID getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(UUID idPersona) {
        this.idPersona = idPersona;
    }

    public UUID getIdTipoMedioContacto() {
        return idTipoMedioContacto;
    }

    public void setIdTipoMedioContacto(UUID idTipoMedioContacto) {
        this.idTipoMedioContacto = idTipoMedioContacto;
    }

    @Override
    public UUID id() {
        return idMedioContacto;
    }
}
