package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol;
import java.util.UUID;

@Stateless
public class RolDAO extends DefaultDAO<Rol, UUID> {

    public RolDAO() {
        super(Rol.class);
    }
}
