package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.UUID;
@Named @SessionScoped
public class AtencionSesion implements Serializable {
    private UUID consulta;
    public UUID getConsulta() { return consulta; }
    public void setConsulta(UUID consulta) { this.consulta=consulta; }
}
