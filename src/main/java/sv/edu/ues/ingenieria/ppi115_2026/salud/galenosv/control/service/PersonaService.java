package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.MedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.PersonaRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

@Stateless
public class PersonaService extends AbstractService<Persona, UUID> {

    public static boolean nombreValido(String nombre) {
        return nombre != null && nombre.strip().matches("\\p{L}[\\p{L}\\p{M}]*(?: +\\p{L}[\\p{L}\\p{M}]*)*");
    }

    @Inject
    private PersonaRepository personaRepository;

    @PersistenceContext
    private EntityManager em;

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
        if (!nombreValido(entidad.getNombres()) || !nombreValido(entidad.getApellidos())) {
            throw new ServiceException("persona.nombreInvalido",
                    "Los nombres y apellidos solo admiten letras y espacios.", null);
        }
        if (entidad.getFechaNacimiento() == null) {
            throw new ServiceException("persona.requerido.fecha", "La fecha de nacimiento es obligatoria.", null);
        }
        entidad.setNombres(entidad.getNombres().strip());
        entidad.setApellidos(entidad.getApellidos().strip());
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

    /** Elimina los datos personales en una transacción, conservando el historial clínico. */
    @Override
    public void eliminar(UUID id) {
        if (id == null) {
            throw new ServiceException("error.noExisteEliminar", "No existe la persona que se desea eliminar.", null);
        }
        try {
            Persona persona = em.find(Persona.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (persona == null) {
                throw new ServiceException("error.noExisteEliminar", "No existe la persona que se desea eliminar.", null);
            }

            // Bloquear también las asignaciones antes de comprobar sus usos clínicos.
            List<PersonaRol> asignaciones = em.createQuery(
                    "SELECT pr FROM PersonaRol pr WHERE pr.idPersona = :persona ORDER BY pr.idPersonaRol", PersonaRol.class)
                    .setParameter("persona", persona)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
            long consultas = em.createQuery(
                    "SELECT COUNT(c) FROM Consulta c WHERE c.idPersonaRol.idPersona = :persona", Long.class)
                    .setParameter("persona", persona).getSingleResult();
            long pasos = em.createQuery(
                    "SELECT COUNT(p) FROM ConsultaProcedimientoPaso p WHERE p.idPersonaRol.idPersona = :persona", Long.class)
                    .setParameter("persona", persona).getSingleResult();
            if (consultas > 0 || pasos > 0) {
                throw new ServiceException("persona.eliminar.historial",
                        "No se puede eliminar la persona porque tiene consultas o pasos clínicos registrados.", null);
            }

            for (Documento documento : em.createQuery(
                    "SELECT d FROM Documento d WHERE d.idPersona = :persona", Documento.class)
                    .setParameter("persona", persona).getResultList()) {
                em.remove(documento);
            }
            for (MedioContacto contacto : em.createQuery(
                    "SELECT m FROM MedioContacto m WHERE m.idPersona = :persona", MedioContacto.class)
                    .setParameter("persona", persona).getResultList()) {
                em.remove(contacto);
            }
            for (PersonaRol asignacion : asignaciones) {
                em.remove(asignacion);
            }
            // Resolver las claves foráneas antes del DELETE de persona.
            em.flush();
            em.remove(persona);
            em.flush();
        } catch (PersistenceException e) {
            // Traducir dentro de este EJB antes de que el contenedor envuelva la excepción.
            throw new ServiceException("persona.eliminar.error", "No se pudo eliminar la persona.", e);
        }
    }

}
