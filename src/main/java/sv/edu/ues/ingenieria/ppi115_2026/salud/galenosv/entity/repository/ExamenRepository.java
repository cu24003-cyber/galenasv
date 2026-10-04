package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Examen;

@Stateless
public class ExamenRepository extends AbstractRepository<Examen, UUID> {

    public ExamenRepository() {
        super(Examen.class);
    }
}
