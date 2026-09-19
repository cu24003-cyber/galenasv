package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.RolDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol;
import java.util.UUID;


@Stateless
public class RolService extends AbstractService<Rol, UUID> {

    @Inject
    private RolDAO rolDAO;

    @Override
    protected DefaultDAOInterface<Rol, UUID> getDao() {
        return rolDAO;
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
