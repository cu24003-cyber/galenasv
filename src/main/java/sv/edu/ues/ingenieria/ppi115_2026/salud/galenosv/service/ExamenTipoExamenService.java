package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.ExamenTipoExamenDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenTipoExamen;
import java.util.UUID;


@Stateless
public class ExamenTipoExamenService extends AbstractService<ExamenTipoExamen, UUID> {

    @Inject
    private ExamenTipoExamenDAO examenTipoExamenDAO;

    @Override
    protected DefaultDAOInterface<ExamenTipoExamen, UUID> getDao() {
        return examenTipoExamenDAO;
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
