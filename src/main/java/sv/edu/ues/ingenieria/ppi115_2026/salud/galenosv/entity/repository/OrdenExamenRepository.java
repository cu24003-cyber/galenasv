package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.OrdenExamen;

@Stateless
public class OrdenExamenRepository extends AbstractRepository<OrdenExamen, UUID> {

    public OrdenExamenRepository() {
        super(OrdenExamen.class);
    }
}
