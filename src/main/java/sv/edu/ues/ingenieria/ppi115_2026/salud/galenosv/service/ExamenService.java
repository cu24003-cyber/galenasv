package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.ExamenDAO;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen;
import java.util.UUID;


@Stateless
public class ExamenService extends AbstractService<Examen, UUID> {

    @Inject
    private ExamenDAO examenDAO;

    @Override
    protected DefaultDAOInterface<Examen, UUID> getDao() {
        return examenDAO;
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
