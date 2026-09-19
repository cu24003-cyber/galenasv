package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.ProcedimientoPasoSecuenciaDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoSecuencia;
import java.util.UUID;


@Stateless
public class ProcedimientoPasoSecuenciaService extends AbstractService<ProcedimientoPasoSecuencia, UUID> {

    @Inject
    private ProcedimientoPasoSecuenciaDAO procedimientoPasoSecuenciaDAO;

    @Override
    protected DefaultDAOInterface<ProcedimientoPasoSecuencia, UUID> getDao() {
        return procedimientoPasoSecuenciaDAO;
    }

    @Override
    protected UUID obtenerId(ProcedimientoPasoSecuencia entidad) {
        return entidad.getIdProcedimientoPasoSecuencia();
    }

    @Override
    public void crear(ProcedimientoPasoSecuencia entidad) {
        if (entidad.getIdProcedimientoPasoSecuencia() == null) {
            entidad.setIdProcedimientoPasoSecuencia(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
