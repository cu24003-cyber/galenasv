package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;

import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.AbstractService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

@Named
@ViewScoped
public class PersonaModel implements Serializable {

    @EJB
    private PersonaService personaService;

    @Override
    protected AbstractService<Persona, UUID> getService() {
        return personaService;
    private List<Persona> personas;
    private Persona seleccionada;

    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println("=================================");
        System.out.println("PersonaModel @PostConstruct");
        System.out.println("PersonaService: " + personaService);
        System.out.println("=================================");
        cargarPersonas();
    }

    private void cargarPersonas() {
        personas = personaService.listarTodos();
    }

    public void nuevo() {
         System.out.println(">>> PersonaModel.nuevo()");
        seleccionada = new Persona();
    }

    @Override
    protected Persona nuevaInstancia() {
        return new Persona();
    public void editar(Persona p) {
         System.out.println(">>> PersonaModel.editar()");
        seleccionada = p;
    }

    @Override
    protected UUID obtenerId(Persona entidad) {
        return entidad.getIdPersona();
    public void guardar() {
         System.out.println(">>> PersonaModel.guardar()");
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

    // Wrapper para mantener el binding #{personaModel.personas} del .xhtml sin cambios
    public List<Persona> getPersonas() {
        return getRegistros();
    }
}