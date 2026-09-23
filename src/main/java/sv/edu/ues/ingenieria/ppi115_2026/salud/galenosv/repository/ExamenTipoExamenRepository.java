package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenTipoExamen;
import java.util.UUID;

@Stateless
public class ExamenTipoExamenRepository extends AbstractRepository<ExamenTipoExamen, UUID> {

    public ExamenTipoExamenRepository() {
        super(ExamenTipoExamen.class);
    }
}
