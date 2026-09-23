package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.MedioContactoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.MedioContacto;
import java.util.UUID;


@Stateless
public class MedioContactoService extends AbstractService<MedioContacto, UUID> {

    @Inject
    private MedioContactoRepository medioContactoRepository;

    @Override
    protected RepositoryInterface<MedioContacto, UUID> getRepository() {
        return medioContactoRepository;
    }

    @Override
    protected UUID obtenerId(MedioContacto entidad) {
        return entidad.getIdMedioContacto();
    }

    @Override
    public void crear(MedioContacto entidad) {
        if (entidad.getIdMedioContacto() == null) {
            entidad.setIdMedioContacto(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
