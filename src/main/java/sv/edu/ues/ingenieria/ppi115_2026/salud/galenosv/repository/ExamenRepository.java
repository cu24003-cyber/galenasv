package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen;
import java.util.UUID;

@Stateless
public class ExamenRepository extends AbstractRepository<Examen, UUID> {

    public ExamenRepository() {
        super(Examen.class);
    }
}
