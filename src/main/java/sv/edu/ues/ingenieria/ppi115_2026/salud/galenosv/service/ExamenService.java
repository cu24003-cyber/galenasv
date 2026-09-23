package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ExamenRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen;
import java.util.UUID;


@Stateless
public class ExamenService extends AbstractService<Examen, UUID> {

    @Inject
    private ExamenRepository examenRepository;

    @Override
    protected RepositoryInterface<Examen, UUID> getRepository() {
        return examenRepository;
    }

    @Override
    protected UUID obtenerId(Examen entidad) {
        return entidad.getIdExamen();
    }

    @Override
    public void crear(Examen entidad) {
        if (entidad.getIdExamen() == null) {
            entidad.setIdExamen(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
