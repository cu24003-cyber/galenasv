package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

/** Cuerpos de las acciones del flujo; las fechas y los estados los fija el servicio. */
public final class AtencionDto {
    private AtencionDto() {}

    /** Asignación de empleado de la cuenta; {@code activa} indica si es el rol activo de la sesión. */
    /** B8: codigoRol es null (no se omite) en un rol de empleado creado sin código. */
    @jakarta.json.bind.annotation.JsonbNillable
    public record Asignacion(UUID idPersonaRol, String codigoRol, String nombreRol,
            UUID idClinica, String nombreClinica, boolean activa) {
        public static Asignacion de(PersonaRol a, UUID idActivo) {
            return new Asignacion(a.getIdPersonaRol(), a.getIdRol().getCodigo(), a.getIdRol().getNombre(),
                    a.getIdClinica().getIdClinica(), a.getIdClinica().getNombre(),
                    a.getIdPersonaRol().equals(idActivo));
        }
    }

    public static class Responsable {
        @NotNull private UUID idPersonaRol;
        public UUID getIdPersonaRol() { return idPersonaRol; }
        public void setIdPersonaRol(UUID value) { idPersonaRol = value; }
    }

    public static class Notas {
        @Size(max = 255) private String referenciaExterna;
        @Size(max = 255) private String observaciones;
        public String getReferenciaExterna() { return referenciaExterna; }
        public void setReferenciaExterna(String value) { referenciaExterna = value; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String value) { observaciones = value; }
    }

    public static class Finalizacion {
        private String valor;
        public String getValor() { return valor; }
        public void setValor(String value) { valor = value; }
    }

    public static class Examen {
        @NotBlank @Size(max = 155) private String nombre;
        @Size(max = 255) private String observaciones;
        @NotNull private UUID idTipoExamen;
        public String getNombre() { return nombre; }
        public void setNombre(String value) { nombre = value; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String value) { observaciones = value; }
        public UUID getIdTipoExamen() { return idTipoExamen; }
        public void setIdTipoExamen(UUID value) { idTipoExamen = value; }
    }
}
