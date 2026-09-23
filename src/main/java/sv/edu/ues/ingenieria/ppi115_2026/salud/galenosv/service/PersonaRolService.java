package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.PersonaRolRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import java.util.UUID;


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
