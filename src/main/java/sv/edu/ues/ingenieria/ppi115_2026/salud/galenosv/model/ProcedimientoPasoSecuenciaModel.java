package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoSecuencia;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ProcedimientoPasoSecuenciaService;

@Named("procedimientoPasoSecuenciaModel")
@ViewScoped
public class ProcedimientoPasoSecuenciaModel extends AbstractModel<ProcedimientoPasoSecuencia, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ProcedimientoPasoSecuenciaService procedimientoPasoSecuenciaService;

    @Override
    protected ProcedimientoPasoSecuenciaService getService() {
        return procedimientoPasoSecuenciaService;
    }

    @Override
    protected ProcedimientoPasoSecuencia nuevaInstancia() {
        return new ProcedimientoPasoSecuencia();
    }

    @Override
    protected UUID obtenerId(ProcedimientoPasoSecuencia entidad) {
        return entidad.getIdProcedimientoPasoSecuencia();
    }
}
