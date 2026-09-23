package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen;
import java.util.UUID;

@Stateless
public class TipoExamenRepository extends AbstractRepository<TipoExamen, UUID> {

    public TipoExamenRepository() {
        super(TipoExamen.class);
    }
}
