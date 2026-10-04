package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ExamenTipoExamen;

@Stateless
public class ExamenTipoExamenRepository extends AbstractRepository<ExamenTipoExamen, UUID> {

    public ExamenTipoExamenRepository() {
        super(ExamenTipoExamen.class);
    }
}
