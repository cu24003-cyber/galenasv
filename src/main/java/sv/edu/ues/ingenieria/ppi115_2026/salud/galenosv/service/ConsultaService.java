package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.ConsultaDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Consulta;
import java.util.UUID;


@Stateless
public class ConsultaService extends AbstractService<Consulta, UUID> {

    @Inject
    private ConsultaDAO consultaDAO;

    @Override
    protected DefaultDAOInterface<Consulta, UUID> getDao() {
        return consultaDAO;
    }

    @Override
    protected UUID obtenerId(Consulta entidad) {
        return entidad.getIdConsulta();
    }

    @Override
    public void crear(Consulta entidad) {
        if (entidad.getIdConsulta() == null) {
            entidad.setIdConsulta(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
