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
    @EJB private AtencionService atencion;
    @jakarta.inject.Inject private AtencionSesion sesion;
    private Persona seleccionado;
    private String rolId;
    private List<sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol> roles = List.of();
    public void seleccionar() {
        roles = atencion.pacientes(seleccionado.getIdPersona());
        rolId = roles.size()==1 ? roles.get(0).getIdPersonaRol().toString() : null;
    }
    public String abrir() {
        try {
            if (sesion.getConsulta()==null) {
                if (roles.stream().noneMatch(r->r.getIdPersonaRol().toString().equals(rolId))) throw new ServiceException("Seleccione la clínica del paciente.");
                sesion.setConsulta(atencion.abrir(java.util.UUID.fromString(rolId)).getIdConsulta());
            }
            return "/paginas/consulta.xhtml?faces-redirect=true";
        } catch (ServiceException | EJBException | IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR,"No se pudo abrir la consulta. Seleccione una asignación válida del paciente.",null)); return null;
        }
    }
    public Persona getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Persona p) { seleccionado=p; }
    public String getRolId() { return rolId; }
    public void setRolId(String id) { rolId=id; }
    public List<sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol> getRoles() { return roles; }
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
