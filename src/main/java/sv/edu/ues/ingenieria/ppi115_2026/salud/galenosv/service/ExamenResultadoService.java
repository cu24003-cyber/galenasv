package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.ExamenResultadoDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenResultado;
import java.util.UUID;


@Stateless
public class ExamenResultadoService extends AbstractService<ExamenResultado, UUID> {

    @Inject
    private ExamenResultadoDAO examenResultadoDAO;

    @Override
    protected DefaultDAOInterface<ExamenResultado, UUID> getRepository() {
        return examenResultadoDAO;
    }

    @Override
    protected UUID obtenerId(ExamenResultado entidad) {
        return entidad.getIdExamenResultado();
    }

    @Override
    public void crear(ExamenResultado entidad) {
        if (entidad.getIdExamenResultado() == null) {
            entidad.setIdExamenResultado(UUID.randomUUID());
        }
        super.crear(entidad);
    }
    public java.util.List<sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenResultado> listarPorOrdenExamen(UUID idOrdenExamen) {
        return examenResultadoDAO.listarPorOrdenExamen(idOrdenExamen);
    }
}
