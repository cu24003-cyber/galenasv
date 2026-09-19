package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.OrdenExamen;
import java.util.UUID;

@Stateless
public class OrdenExamenDAO extends DefaultDAO<OrdenExamen, UUID> {

    public OrdenExamenDAO() {
        super(OrdenExamen.class);
    }
}
