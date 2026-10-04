package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RolRepository;

@Stateless(name = "RolService")
@LocalBean
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
