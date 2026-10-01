package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ContextoRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

@Named @ViewScoped
public class CambiarRolModel implements Serializable {
    private static final long serialVersionUID = 1L;
    @EJB private ContextoRolService servicio;
    @Inject private AtencionSesion sesion;
    private List<PersonaRol> asignaciones = List.of();
    private String asignacionId;

    @PostConstruct public void iniciar() {
        try {
            asignaciones = servicio.listar();
            if (sesion.getRolActivo() != null) asignacionId = sesion.getRolActivo().getIdPersonaRol().toString();
        } catch (ServiceException | EJBException e) { error("No se pudieron cargar los roles disponibles."); }
    }

    public String aplicar() {
        try {
            if (asignacionId == null || asignacionId.isBlank()) throw new ServiceException("Seleccione una asignación de persona, rol y clínica.");
            sesion.cambiarRol(servicio.cargar(UUID.fromString(asignacionId)));
            return sesion.getConsulta() == null ? "/paginas/paciente-list.xhtml?faces-redirect=true"
                    : "/paginas/consulta.xhtml?faces-redirect=true";
        } catch (ServiceException e) { error(e.getMessage()); }
        catch (EJBException | IllegalArgumentException e) { error("Seleccione una asignación válida de persona, rol y clínica."); }
        return null;
    }

    private void error(String texto) {
        FacesContext contexto = FacesContext.getCurrentInstance();
        contexto.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, texto, null));
        contexto.validationFailed();
    }
    public List<PersonaRol> getAsignaciones() { return asignaciones; }
    public String getAsignacionId() { return asignacionId; }
    public void setAsignacionId(String id) { asignacionId = id; }
}
