package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.PersonaRolDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import java.util.UUID;


@Stateless
public class PersonaRolService extends AbstractService<PersonaRol, UUID> {

    @Inject
    private PersonaRolDAO personaRolDAO;

    @Override
    protected DefaultDAOInterface<PersonaRol, UUID> getRepository() {
        return personaRolDAO;
    }

    @Override
    protected UUID obtenerId(PersonaRol entidad) {
        return entidad.getIdPersonaRol();
    }

    @Override
    public void crear(PersonaRol entidad) {
        if (entidad.getIdPersonaRol() == null) {
            entidad.setIdPersonaRol(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
