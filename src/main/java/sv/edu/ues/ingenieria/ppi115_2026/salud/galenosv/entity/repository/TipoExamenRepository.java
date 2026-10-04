package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoExamen;

@Stateless
public class TipoExamenRepository extends AbstractRepository<TipoExamen, UUID> {

    public TipoExamenRepository() {
        super(TipoExamen.class);
    }
}
