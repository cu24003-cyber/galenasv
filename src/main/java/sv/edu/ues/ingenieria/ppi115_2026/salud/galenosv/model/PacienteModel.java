package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;

@Named
@ViewScoped
public class PacienteModel implements Serializable {
    private static final long serialVersionUID = 1L;
    @EJB private RegistroPersonaService servicio;
    private List<Persona> pacientes = List.of();
    @PostConstruct
    public void cargar() {
        try { pacientes = servicio.listarPacientes(); }
        catch (ServiceException | EJBException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, Mensajes.texto("error.general"), null));
        }
    }
    public List<Persona> getPacientes() { return pacientes; }
}
