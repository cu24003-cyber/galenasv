package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenResultado;
import java.util.UUID;

@Stateless
public class ExamenResultadoDAO extends DefaultDAO<ExamenResultado, UUID> {

    public ExamenResultadoDAO() {
        super(ExamenResultado.class);
    }
}
