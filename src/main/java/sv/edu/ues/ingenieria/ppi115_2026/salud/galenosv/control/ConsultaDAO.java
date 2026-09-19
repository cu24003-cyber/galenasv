package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Consulta;
import java.util.UUID;

@Stateless
public class ConsultaDAO extends DefaultDAO<Consulta, UUID> {

    public ConsultaDAO() {
        super(Consulta.class);
    }
}
