package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoMedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.TipoMedioContactoRepository;

@Stateless(name = "TipoMedioContactoService")
@LocalBean
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

    public boolean estaEnUso(UUID id) {
        return id != null && tipoMedioContactoRepository.estaEnUso(id);
    }

    @Override
    public void actualizar(TipoMedioContacto entidad) {
        validar(entidad);
        if (estaEnUso(entidad.getIdTipoMedioContacto())) {
            throw ServiceException.localizada("tipoMedioContacto.enUso",
                    "No se puede editar un tipo de contacto que ya tiene medios de contacto asociados.");
        }
        super.actualizar(entidad);
    }

}
