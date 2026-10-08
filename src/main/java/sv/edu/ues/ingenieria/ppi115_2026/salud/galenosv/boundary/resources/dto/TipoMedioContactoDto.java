package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Contrato JSON de TipoMedioContacto; las relaciones se representan mediante UUID. */
public class TipoMedioContactoDto implements IdentifiableDto {

    private UUID idTipoMedioContacto;

    @NotBlank(message = "nombre es obligatorio.")
    @Size(max = 155, message = "nombre admite hasta 155 caracteres.")
    private String nombre;

    @Size(max = 255, message = "indicaciones admite hasta 255 caracteres.")
    private String indicaciones;

    @Size(max = 255, message = "expresionRegular admite hasta 255 caracteres.")
    private String expresionRegular;

    private Boolean activo;

    public UUID getIdTipoMedioContacto() {
        return idTipoMedioContacto;
    }

    public void setIdTipoMedioContacto(UUID idTipoMedioContacto) {
        this.idTipoMedioContacto = idTipoMedioContacto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }

    public String getExpresionRegular() {
        return expresionRegular;
    }

    public void setExpresionRegular(String expresionRegular) {
        this.expresionRegular = expresionRegular;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    @Override
    public UUID id() {
        return idTipoMedioContacto;
    }
}
