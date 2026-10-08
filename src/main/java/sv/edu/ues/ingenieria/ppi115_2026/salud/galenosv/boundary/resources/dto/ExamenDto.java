package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Contrato JSON de Examen; las relaciones se representan mediante UUID. */
public class ExamenDto implements IdentifiableDto {

    private UUID idExamen;

    @NotBlank(message = "nombre es obligatorio.")
    @Size(max = 155, message = "nombre admite hasta 155 caracteres.")
    private String nombre;

    private Boolean activo;

    @Size(max = 255, message = "observaciones admite hasta 255 caracteres.")
    private String observaciones;

    public UUID getIdExamen() {
        return idExamen;
    }

    public void setIdExamen(UUID idExamen) {
        this.idExamen = idExamen;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public UUID id() {
        return idExamen;
    }
}
