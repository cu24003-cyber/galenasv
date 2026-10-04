package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

@Stateless(name = "RolRepository")
@LocalBean
public class RolRepository extends AbstractRepository<Rol, UUID> {

    public RolRepository() {
        super(Rol.class);
    }
}
