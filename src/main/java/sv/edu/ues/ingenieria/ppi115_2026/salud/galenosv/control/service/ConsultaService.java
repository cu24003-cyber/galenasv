package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Consulta;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ConsultaRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

@Stateless
public class ConsultaService extends AbstractService<Consulta, UUID> {

    @Inject
    private ConsultaRepository consultaRepository;

    @Override
    protected RepositoryInterface<Consulta, UUID> getRepository() {
        return consultaRepository;
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
    public java.util.List<sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Consulta> listarPorPersona(UUID idPersona) {
        return consultaRepository.listarPorPersona(idPersona);
    }
}
