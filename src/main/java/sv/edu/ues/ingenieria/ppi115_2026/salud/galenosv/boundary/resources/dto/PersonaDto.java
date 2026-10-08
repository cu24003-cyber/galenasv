package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.UUID;

/** Contrato JSON de Persona; las relaciones se representan mediante UUID. */
public class PersonaDto implements IdentifiableDto {

    private UUID idPersona;

    @NotBlank(message = "nombres es obligatorio.")
    @Size(max = 255, message = "nombres admite hasta 255 caracteres.")
    private String nombres;

    @NotBlank(message = "apellidos es obligatorio.")
    @Size(max = 255, message = "apellidos admite hasta 255 caracteres.")
    private String apellidos;

    @NotNull(message = "fechaNacimiento es obligatorio.")
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date fechaNacimiento;

    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date fechaCreacion;

    public UUID getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(UUID idPersona) {
        this.idPersona = idPersona;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public UUID id() {
        return idPersona;
    }
}
