package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.AbstractService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaService;

@Named
@ViewScoped
public class PersonaModel extends AbstractModel<Persona, UUID> implements Serializable {

    @EJB
    private PersonaService personaService;

    @Override
    protected AbstractService<Persona, UUID> getService() {
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

    // Wrapper para mantener el binding #{personaModel.personas} del .xhtml sin cambios
    public List<Persona> getPersonas() {
        return getRegistros();
    }
}