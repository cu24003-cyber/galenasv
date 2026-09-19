package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenTipoExamen;
import java.util.UUID;

@Stateless
public class ExamenTipoExamenDAO extends DefaultDAO<ExamenTipoExamen, UUID> {

    public ExamenTipoExamenDAO() {
        super(ExamenTipoExamen.class);
    }
}
