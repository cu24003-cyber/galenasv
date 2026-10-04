package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimientoPaso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ConsultaProcedimientoPasoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

@Stateless
public class ConsultaProcedimientoPasoService extends AbstractService<ConsultaProcedimientoPaso, UUID> {

    @Inject
    private ConsultaProcedimientoPasoRepository consultaProcedimientoPasoRepository;

    @Override
    protected RepositoryInterface<ConsultaProcedimientoPaso, UUID> getRepository() {
        return consultaProcedimientoPasoRepository;
    }

    @Override
    protected UUID obtenerId(ConsultaProcedimientoPaso entidad) {
        return entidad.getIdConsultaProcedimientoPaso();
    }

    @Override
    public void crear(ConsultaProcedimientoPaso entidad) {
        if (entidad.getIdConsultaProcedimientoPaso() == null) {
            entidad.setIdConsultaProcedimientoPaso(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
