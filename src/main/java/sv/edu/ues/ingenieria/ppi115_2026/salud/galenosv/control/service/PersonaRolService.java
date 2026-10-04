package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.PersonaRolRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

@Stateless
public class PersonaRolService extends AbstractService<PersonaRol, UUID> {

    @Inject
    private PersonaRolRepository personaRolRepository;

    @Override
    protected RepositoryInterface<PersonaRol, UUID> getRepository() {
        return personaRolRepository;
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
