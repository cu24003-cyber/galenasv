package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ConsultaProcedimientoPasoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimientoPaso;
import java.util.UUID;


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
