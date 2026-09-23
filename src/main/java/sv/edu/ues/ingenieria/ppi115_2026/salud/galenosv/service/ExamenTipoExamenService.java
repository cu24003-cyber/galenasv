package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ExamenTipoExamenRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenTipoExamen;
import java.util.UUID;


@Stateless
public class ExamenTipoExamenService extends AbstractService<ExamenTipoExamen, UUID> {

    @Inject
    private ExamenTipoExamenRepository examenTipoExamenRepository;

    @Override
    protected RepositoryInterface<ExamenTipoExamen, UUID> getRepository() {
        return examenTipoExamenRepository;
    }

    @Override
    protected UUID obtenerId(ExamenTipoExamen entidad) {
        return entidad.getIdExamenTipoExamen();
    }

    @Override
    public void crear(ExamenTipoExamen entidad) {
        if (entidad.getIdExamenTipoExamen() == null) {
            entidad.setIdExamenTipoExamen(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
