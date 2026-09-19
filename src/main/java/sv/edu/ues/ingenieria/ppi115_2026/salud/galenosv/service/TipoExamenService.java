package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.TipoExamenDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen;
import java.util.UUID;


@Stateless
public class TipoExamenService extends AbstractService<TipoExamen, UUID> {

    @Inject
    private TipoExamenDAO tipoExamenDAO;

    @Override
    protected DefaultDAOInterface<TipoExamen, UUID> getDao() {
        return tipoExamenDAO;
    }

    @Override
    protected UUID obtenerId(TipoExamen entidad) {
        return entidad.getIdTipoExamen();
    }

    @Override
    public void crear(TipoExamen entidad) {
        if (entidad.getIdTipoExamen() == null) {
            entidad.setIdTipoExamen(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
