package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

/** Contrato JSON de PersonaRol; las relaciones se representan mediante UUID. */
public class PersonaRolDto implements IdentifiableDto {

    private UUID idPersonaRol;

    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date fechaCreacion;

    @NotNull(message = "idClinica es obligatorio.")
    private UUID idClinica;

    @NotNull(message = "idPersona es obligatorio.")
    private UUID idPersona;

    @NotNull(message = "idRol es obligatorio.")
    private UUID idRol;

    public UUID getIdPersonaRol() {
        return idPersonaRol;
    }

    public void setIdPersonaRol(UUID idPersonaRol) {
        this.idPersonaRol = idPersonaRol;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public UUID getIdClinica() {
        return idClinica;
    }

    public void setIdClinica(UUID idClinica) {
        this.idClinica = idClinica;
    }

    public UUID getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(UUID idPersona) {
        this.idPersona = idPersona;
    }

    public UUID getIdRol() {
        return idRol;
    }

    public void setIdRol(UUID idRol) {
        this.idRol = idRol;
    }

    @Override
    public UUID id() {
        return idPersonaRol;
    }
}
