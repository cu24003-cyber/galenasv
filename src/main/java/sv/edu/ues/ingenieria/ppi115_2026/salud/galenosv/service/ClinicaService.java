package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ClinicaRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;

import java.util.List;
import java.util.UUID;


@Stateless
public class ClinicaService extends AbstractService<Clinica, UUID> {

    @Inject
    private ClinicaRepository clinicaRepository;

    @Override
    protected RepositoryInterface<Clinica, UUID> getRepository() {
        return clinicaRepository;
    }

    public List<Clinica> searchByName(String name){
        try{
            return clinicaRepository.findClinicaByName(name);
        }catch (PersistenceException e){
            throw new ServiceException("error.cargarClinicas","No se pudo buscar por el nombre", e);
        }
    }

    @Override
    protected UUID obtenerId(Clinica entidad) {
        return entidad.getIdClinica();
    }

    @Override
    public void crear(Clinica entidad) {
        if (entidad.getIdClinica() == null) {
            entidad.setIdClinica(UUID.randomUUID());
        }
        super.crear(entidad);
    }

    @Override
    protected void validar(Clinica entidad) {
        super.validar(entidad);
        if (entidad.getNombre() == null || entidad.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la clinica es obligatorio.");
        }
    }
}
