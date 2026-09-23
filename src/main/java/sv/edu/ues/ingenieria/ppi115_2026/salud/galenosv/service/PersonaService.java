package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.PersonaRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import java.util.UUID;
import java.util.Date;

@Stateless
public class PersonaService extends AbstractService<Persona, UUID> {

    @Inject
    private PersonaRepository personaRepository;

    @Override
    protected RepositoryInterface<Persona, UUID> getRepository() {
        return personaRepository;
    }

    @Override
    protected UUID obtenerId(Persona entidad) {
        return entidad.getIdPersona();
    }

    @Override
    public void crear(Persona entidad) {
        if (entidad.getIdPersona() == null) {
            entidad.setIdPersona(UUID.randomUUID());
        }
        if (entidad.getFechaCreacion() == null) {
            entidad.setFechaCreacion(new Date());
        }
        super.crear(entidad);
    }

}
