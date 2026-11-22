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

    /** Sin UNIQUE en la base original: no se repite persona + rol + clínica. */
    private void validarUnica(PersonaRol entidad, UUID excluir) {
        if (entidad == null || entidad.getIdPersona() == null || entidad.getIdPersona().getIdPersona() == null
                || entidad.getIdClinica() == null || entidad.getIdClinica().getIdClinica() == null) return;
        UUID rol = entidad.getIdRol() == null ? null : entidad.getIdRol().getIdRol();
        if (personaRolRepository.existeAsignacion(entidad.getIdPersona().getIdPersona(), rol,
                entidad.getIdClinica().getIdClinica(), excluir)) {
            throw ServiceException.localizada("personaRol.duplicada",
                    "La persona ya tiene ese rol en esa clínica.");
        }
    }

    @Override
    public void crear(PersonaRol entidad) {
        validarUnica(entidad, null);
        if (entidad.getIdPersonaRol() == null) {
            entidad.setIdPersonaRol(UUID.randomUUID());
        }
        super.crear(entidad);
    }

    @Override
    public void actualizar(PersonaRol entidad) {
        validarUnica(entidad, entidad == null ? null : entidad.getIdPersonaRol());
        super.actualizar(entidad);
    }

}
