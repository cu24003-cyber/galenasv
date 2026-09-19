package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen;
import java.util.UUID;

@Stateless
public class TipoExamenDAO extends DefaultDAO<TipoExamen, UUID> {

    public TipoExamenDAO() {
        super(TipoExamen.class);
    }
}
