package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Contrato JSON de Clinica; las relaciones se representan mediante UUID. */
public class ClinicaDto implements IdentifiableDto {

    private UUID idClinica;

    @NotBlank(message = "nombre es obligatorio.")
    @Size(max = 255, message = "nombre admite hasta 255 caracteres.")
    private String nombre;

    private Boolean activo;

    @Size(max = 20, message = "tipo admite hasta 20 caracteres.")
    private String tipo;

    @Size(max = 255, message = "comentarios admite hasta 255 caracteres.")
    private String comentarios;

    public UUID getIdClinica() {
        return idClinica;
    }

    public void setIdClinica(UUID idClinica) {
        this.idClinica = idClinica;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    @Override
    public UUID id() {
        return idClinica;
    }
}
