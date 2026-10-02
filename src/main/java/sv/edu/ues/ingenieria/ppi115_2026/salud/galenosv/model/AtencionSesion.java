package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

@Named @SessionScoped
public class AtencionSesion implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID consulta;
    private PersonaRol rolActivo;

    public PersonaRol getRolActivo() { return rolActivo; }

    public void cambiarRol(PersonaRol rol) {
        if (rol == null || rol.getIdPersonaRol() == null || rol.getIdPersona() == null
                || rol.getIdClinica() == null || rol.getIdRol() == null
                || !Boolean.TRUE.equals(rol.getIdRol().getActivo())) {
            throw new ServiceException("Seleccione una asignación con un rol activo y una clínica.");
        }
        if (consulta != null && (rolActivo == null || !rolActivo.getIdPersonaRol().equals(rol.getIdPersonaRol()))) {
            throw new ServiceException("Cierre la consulta antes de cambiar de médico o rol.");
        }
        rolActivo = rol;
    }

    public UUID getConsulta() { return consulta; }
    public void setConsulta(UUID consulta) { this.consulta = consulta; }
}
