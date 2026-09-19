package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.MedioContactoDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.MedioContacto;
import java.util.UUID;


@Stateless
public class MedioContactoService extends AbstractService<MedioContacto, UUID> {

    @Inject
    private MedioContactoDAO medioContactoDAO;

    @Override
    protected DefaultDAOInterface<MedioContacto, UUID> getDao() {
        return medioContactoDAO;
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
