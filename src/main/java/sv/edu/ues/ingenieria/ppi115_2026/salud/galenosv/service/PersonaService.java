package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.PersonaRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import java.util.UUID;
import java.util.Date;
import java.util.List;
import jakarta.persistence.PersistenceException;

@Stateless
public class PersonaService extends AbstractService<Persona, UUID> {

    @Inject
    private PersonaRepository personaRepository;

    @Override
    protected RepositoryInterface<Persona, UUID> getRepository() {
        return personaRepository;
    }

    public List<Persona> buscarPorNombre(String nombre) {
        try {
            return personaRepository.buscarPorNombre(nombre);
        } catch (PersistenceException e) {
            throw new ServiceException("error.cargarPersonas", "No se pudo buscar personas por nombre.", e);
        }
    }

    @Override
    protected void validar(Persona entidad) {
        super.validar(entidad);
        java.time.LocalDate hoy = java.time.LocalDate.now(java.time.ZoneId.of("America/El_Salvador"));
        for (Date fecha : new Date[] {entidad.getFechaNacimiento(), entidad.getFechaCreacion()}) {
            if (fecha != null && java.time.Instant.ofEpochMilli(fecha.getTime())
                    .atZone(java.time.ZoneId.of("America/El_Salvador")).toLocalDate().isAfter(hoy)) {
                throw new ServiceException("fecha.futura", "La fecha no puede ser posterior a la fecha actual.", null);
            }
        }
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
