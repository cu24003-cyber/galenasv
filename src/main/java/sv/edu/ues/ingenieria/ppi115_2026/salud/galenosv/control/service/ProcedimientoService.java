package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Procedimiento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ProcedimientoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

@Stateless
public class ProcedimientoService extends AbstractService<Procedimiento, UUID> {

    @Inject
    private ProcedimientoRepository procedimientoRepository;

    @Override
    protected RepositoryInterface<Procedimiento, UUID> getRepository() {
        return procedimientoRepository;
    }

    @Override
    protected UUID obtenerId(Procedimiento entidad) {
        return entidad.getIdProcedimiento();
    }

    @Override
    public void crear(Procedimiento entidad) {
        if (entidad.getIdProcedimiento() == null) {
            entidad.setIdProcedimiento(UUID.randomUUID());
        }
        super.crear(entidad);
    }

    @Override
    public void eliminar(UUID id) {
        if (id == null || procedimientoRepository.bloquear(id) == null) {
            throw new ServiceException("error.noExisteEliminar", "El procedimiento no existe.", null);
        }
        if (procedimientoRepository.registradoEnConsultas(id)) {
            throw new ServiceException("procedimiento.eliminarEnUso",
                    "No se puede eliminar un procedimiento registrado en una consulta.", null);
        }
        try {
            procedimientoRepository.eliminarConfiguracion(id);
            procedimientoRepository.delete(id);
        } catch (PersistenceException e) {
            throw new ServiceException("error.relacionado", "No se pudo eliminar el procedimiento por sus relaciones.", e);
        }
    }

}
