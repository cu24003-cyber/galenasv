package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

@Named
@ViewScoped
public class PersonaModel implements Serializable {

    @EJB
    private PersonaService personaService;

    private List<Persona> personas;
    private Persona seleccionada;

    @jakarta.annotation.PostConstruct
    public void init() {
        cargarPersonas();
    }

    private void cargarPersonas() {
        personas = personaService.listarTodos();
    }

    public void nuevo() {
        seleccionada = new Persona();
    }

    public void editar(Persona p) {
        seleccionada = p;
    }

    public void guardar() {
        try {
            if (seleccionada.getIdPersona() == null) {
                personaService.crear(seleccionada);
            } else {
                personaService.actualizar(seleccionada);
            }
            cargarPersonas();
            seleccionada = null;
        } catch (ServiceException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al guardar", e.getMessage()));
        }
    }

    public void eliminar(Persona p) {
        try {
            personaService.eliminar(p.getIdPersona());
            cargarPersonas();
        } catch (ServiceException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al eliminar", e.getMessage()));
        }
    }

    public List<Persona> getPersonas() {
        return personas;
    }

    public Persona getSeleccionada() {
        return seleccionada;
    }

    public void setSeleccionada(Persona seleccionada) {
        this.seleccionada = seleccionada;
    }
}