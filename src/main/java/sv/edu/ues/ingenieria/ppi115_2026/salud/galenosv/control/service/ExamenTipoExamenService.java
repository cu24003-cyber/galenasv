package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ExamenTipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ExamenTipoExamenRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

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
