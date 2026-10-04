package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.TipoExamenRepository;

@Stateless
public class TipoExamenService extends AbstractService<TipoExamen, UUID> {

    @Inject
    private TipoExamenRepository tipoExamenRepository;

    @Override
    protected RepositoryInterface<TipoExamen, UUID> getRepository() {
        return tipoExamenRepository;
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
