package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaService;

@Named("personaModel")
@ViewScoped
public class PersonaModel extends AbstractModel<Persona, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private PersonaService personaService;

    @Override
    protected PersonaService getService() {
        return personaService;
    }

    @Override
    protected Persona nuevaInstancia() {
        return new Persona();
    }

    @Override
    protected UUID obtenerId(Persona entidad) {
        return entidad.getIdPersona();
    }

    public void cargarPersonas() {
        cargarRegistros();
    }

    public java.util.List<Persona> getPersonas() {
        return getRegistros();
    }
}
