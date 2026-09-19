package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.TipoMedioContactoDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import java.util.UUID;


@Stateless
public class TipoMedioContactoService extends AbstractService<TipoMedioContacto, UUID> {

    @Inject
    private TipoMedioContactoDAO tipoMedioContactoDAO;

    @Override
    protected DefaultDAOInterface<TipoMedioContacto, UUID> getDao() {
        return tipoMedioContactoDAO;
    }

    @Override
    protected UUID obtenerId(TipoMedioContacto entidad) {
        return entidad.getIdTipoMedioContacto();
    }

    @Override
    public void crear(TipoMedioContacto entidad) {
        if (entidad.getIdTipoMedioContacto() == null) {
            entidad.setIdTipoMedioContacto(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
