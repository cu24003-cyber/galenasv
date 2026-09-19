package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen;
import java.util.UUID;

@Stateless
public class ExamenDAO extends DefaultDAO<Examen, UUID> {

    public ExamenDAO() {
        super(Examen.class);
    }
}
