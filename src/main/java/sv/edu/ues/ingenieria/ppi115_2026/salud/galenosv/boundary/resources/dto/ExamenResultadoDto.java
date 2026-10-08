package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.UUID;

/** Contrato JSON de ExamenResultado; las relaciones se representan mediante UUID. */
public class ExamenResultadoDto implements IdentifiableDto {

    private UUID idExamenResultado;

    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date fechaCreacion;

    @NotBlank(message = "resultado es obligatorio.")
    @Size(max = 255, message = "resultado admite hasta 255 caracteres.")
    private String resultado;

    @Size(max = 255, message = "interpretacion admite hasta 255 caracteres.")
    private String interpretacion;

    @Size(max = 255, message = "rutaAtestado admite hasta 255 caracteres.")
    private String rutaAtestado;

    @NotNull(message = "idOrdenExamen es obligatorio.")
    private UUID idOrdenExamen;

    public UUID getIdExamenResultado() {
        return idExamenResultado;
    }

    public void setIdExamenResultado(UUID idExamenResultado) {
        this.idExamenResultado = idExamenResultado;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getInterpretacion() {
        return interpretacion;
    }

    public void setInterpretacion(String interpretacion) {
        this.interpretacion = interpretacion;
    }

    public String getRutaAtestado() {
        return rutaAtestado;
    }

    public void setRutaAtestado(String rutaAtestado) {
        this.rutaAtestado = rutaAtestado;
    }

    public UUID getIdOrdenExamen() {
        return idOrdenExamen;
    }

    public void setIdOrdenExamen(UUID idOrdenExamen) {
        this.idOrdenExamen = idOrdenExamen;
    }

    @Override
    public UUID id() {
        return idExamenResultado;
    }
}
