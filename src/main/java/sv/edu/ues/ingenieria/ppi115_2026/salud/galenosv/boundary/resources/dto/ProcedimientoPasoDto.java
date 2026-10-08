package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Contrato JSON de ProcedimientoPaso; las relaciones se representan mediante UUID. */
public class ProcedimientoPasoDto implements IdentifiableDto {

    private UUID idProcedimientoPaso;

    @NotBlank(message = "nombre es obligatorio.")
    @Size(max = 155, message = "nombre admite hasta 155 caracteres.")
    private String nombre;

    private Boolean indicaFin;

    @NotNull(message = "idProcedimiento es obligatorio.")
    private UUID idProcedimiento;

    @NotNull(message = "idRol es obligatorio.")
    private UUID idRol;

    public UUID getIdProcedimientoPaso() {
        return idProcedimientoPaso;
    }

    public void setIdProcedimientoPaso(UUID idProcedimientoPaso) {
        this.idProcedimientoPaso = idProcedimientoPaso;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getIndicaFin() {
        return indicaFin;
    }

    public void setIndicaFin(Boolean indicaFin) {
        this.indicaFin = indicaFin;
    }

    public UUID getIdProcedimiento() {
        return idProcedimiento;
    }

    public void setIdProcedimiento(UUID idProcedimiento) {
        this.idProcedimiento = idProcedimiento;
    }

    public UUID getIdRol() {
        return idRol;
    }

    public void setIdRol(UUID idRol) {
        this.idRol = idRol;
    }

    @Override
    public UUID id() {
        return idProcedimientoPaso;
    }
}
