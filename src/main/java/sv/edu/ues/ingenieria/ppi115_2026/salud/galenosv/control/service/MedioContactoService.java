package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.MedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.MedioContactoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

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

    public java.util.List<MedioContacto> listarPorPersona(UUID idPersona) {
        return medioContactoRepository.listarPorPersona(idPersona);
    }

    public void eliminarDePersona(UUID id, UUID persona) {
        MedioContacto contacto = id == null ? null : medioContactoRepository.find(id);
        if (contacto == null || persona == null || contacto.getIdPersona() == null
                || !persona.equals(contacto.getIdPersona().getIdPersona())) {
            throw ServiceException.localizada("registro.relacionAjena", "El registro no pertenece a la persona seleccionada.");
        }
        super.eliminar(id);
    }
}
