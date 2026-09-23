package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.PersonaDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;

@Named
@ViewScoped
public class PersonaModel implements Serializable {

    @EJB
    private PersonaDAO personaDAO;

    private List<Persona> personas;
    private Persona seleccionada;

    @jakarta.annotation.PostConstruct
    public void init() {
        cargarPersonas();
    }

    private void cargarPersonas() {
        personas = personaDAO.findAll();
    }

    public void nuevo() {
        seleccionada = new Persona();
        seleccionada.setFechaCreacion(new Date());
    }

    public void editar(Persona p) {
        seleccionada = p;
    }

    public void guardar() {
        if (seleccionada.getIdPersona() == null) {
            seleccionada.setIdPersona(UUID.randomUUID());
            personaDAO.create(seleccionada);
        } else {
            personaDAO.update(seleccionada);
        }
        cargarPersonas();
        seleccionada = null;
    }

    public void eliminar(Persona p) {
        personaDAO.delete(p.getIdPersona());
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
