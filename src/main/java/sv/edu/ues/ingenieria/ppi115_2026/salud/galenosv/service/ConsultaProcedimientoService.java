package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ConsultaProcedimientoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimiento;
import java.util.UUID;


@Stateless
public class ConsultaProcedimientoService extends AbstractService<ConsultaProcedimiento, UUID> {

    @Inject
    private ConsultaProcedimientoRepository consultaProcedimientoRepository;

    @Override
    protected RepositoryInterface<ConsultaProcedimiento, UUID> getRepository() {
        return consultaProcedimientoRepository;
    }

    @Override
    protected UUID obtenerId(ConsultaProcedimiento entidad) {
        return entidad.getIdConsultaProcedimiento();
    }

    @Override
    public void crear(ConsultaProcedimiento entidad) {
        if (entidad.getIdConsultaProcedimiento() == null) {
            entidad.setIdConsultaProcedimiento(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
