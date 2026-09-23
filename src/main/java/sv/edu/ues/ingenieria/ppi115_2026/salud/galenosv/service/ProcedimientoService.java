package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.ProcedimientoDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Procedimiento;
import java.util.UUID;


@Stateless
public class ProcedimientoService extends AbstractService<Procedimiento, UUID> {

    @Inject
    private ProcedimientoDAO procedimientoDAO;

    @Override
    protected DefaultDAOInterface<Procedimiento, UUID> getRepository() {
        return procedimientoDAO;
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

}
