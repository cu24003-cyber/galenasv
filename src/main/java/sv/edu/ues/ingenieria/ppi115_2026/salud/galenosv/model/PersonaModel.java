package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

@Named("personaModel")
@ViewScoped
public class PersonaModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PersonaService personaService;

    private List<Persona> personas = new ArrayList<>();
    private Persona seleccionada;

    @PostConstruct
    public void init() {
        cargarPersonas();
    }

    public void cargarPersonas() {
        try {
            personas = personaService.listarTodos();
        } catch (ServiceException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error al cargar personas", e.getMessage()));
        }
    }

    public void nuevo() {
        seleccionada = new Persona();
    }

    public void editar(Persona persona) {
        seleccionada = persona;
    }

    public void guardar() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (seleccionada == null) {
            context.validationFailed();
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Sin selección",
                            "Selecciona una persona o crea una nueva."));
            return;
        }

        try {
            if (seleccionada.getIdPersona() == null) {
                personaService.crear(seleccionada);
            } else {
                personaService.actualizar(seleccionada);
            }
        } catch (ServiceException e) {
            context.validationFailed();
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error al guardar", e.getMessage()));
            return;
        }

        seleccionada = null;
        cargarPersonas();
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