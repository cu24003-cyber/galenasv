package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RolRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol;
import java.util.UUID;


@Stateless
public class RolService extends AbstractService<Rol, UUID> {

    @Inject
    private RolRepository rolRepository;

    @Override
    protected RepositoryInterface<Rol, UUID> getRepository() {
        return rolRepository;
    }

    @Override
    protected UUID obtenerId(Rol entidad) {
        return entidad.getIdRol();
    }

    @Override
    public void crear(Rol entidad) {
        if (entidad.getIdRol() == null) {
            entidad.setIdRol(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
