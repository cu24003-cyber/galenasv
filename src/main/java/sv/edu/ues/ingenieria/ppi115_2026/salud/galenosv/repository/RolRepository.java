package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol;
import java.util.UUID;

@Stateless
public class RolRepository extends AbstractRepository<Rol, UUID> {

    public RolRepository() {
        super(Rol.class);
    }
}
