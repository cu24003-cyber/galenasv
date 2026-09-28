package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaRolService;

@Named("personaRolModel")
@ViewScoped
public class PersonaRolModel extends AbstractModel<PersonaRol, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private PersonaRolService personaRolService;

    @Override
    protected PersonaRolService getService() {
        return personaRolService;
    }

    @Override
    protected PersonaRol nuevaInstancia() {
        return new PersonaRol();
    }

    @Override
    protected UUID obtenerId(PersonaRol entidad) {
        return entidad.getIdPersonaRol();
    }
}
