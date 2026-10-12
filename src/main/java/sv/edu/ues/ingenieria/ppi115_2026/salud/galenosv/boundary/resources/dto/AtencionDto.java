package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Cuerpos de las acciones del flujo; las fechas y los estados los fija el servicio. */
public final class AtencionDto {
    private AtencionDto() {}

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
