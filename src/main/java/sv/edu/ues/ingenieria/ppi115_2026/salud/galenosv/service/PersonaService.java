package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.PersonaDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import java.util.UUID;
import java.util.Date;

@Stateless
public class PersonaService extends AbstractService<Persona, UUID> {

    @Inject
    private PersonaDAO personaDAO;

    @Override
    protected DefaultDAOInterface<Persona, UUID> getRepository() {
        return personaDAO;
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
