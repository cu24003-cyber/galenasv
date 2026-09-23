package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.TipoMedioContactoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import java.util.UUID;


@Stateless
public class TipoMedioContactoService extends AbstractService<TipoMedioContacto, UUID> {

    @Inject
    private TipoMedioContactoRepository tipoMedioContactoRepository;

    @Override
    protected RepositoryInterface<TipoMedioContacto, UUID> getRepository() {
        return tipoMedioContactoRepository;
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
