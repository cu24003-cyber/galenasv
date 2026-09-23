package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.OrdenExamenRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.OrdenExamen;
import java.util.UUID;


@Stateless
public class OrdenExamenService extends AbstractService<OrdenExamen, UUID> {

    @Inject
    private OrdenExamenRepository ordenExamenRepository;

    @Override
    protected RepositoryInterface<OrdenExamen, UUID> getRepository() {
        return ordenExamenRepository;
    }

    @Override
    protected UUID obtenerId(OrdenExamen entidad) {
        return entidad.getIdOrdenExamen();
    }

    @Override
    public void crear(OrdenExamen entidad) {
        if (entidad.getIdOrdenExamen() == null) {
            entidad.setIdOrdenExamen(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
