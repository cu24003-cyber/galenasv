package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

@Stateless(name = "RolRepository")
@LocalBean
public class RolRepository extends AbstractRepository<Rol, UUID> {

    public RolRepository() {
        super(Rol.class);
    }

    /**
     * Rol cuyo nombre normalizado corresponde a {@code codigo} (p. ej. "PACIENTE"), con la misma regla
     * que {@link Rol#getCodigo()}; null si no existe. La unicidad de nombres de RolService garantiza
     * que haya a lo sumo uno. Puede estar inactivo: quien lo use decide qué hacer.
     */
    public static Rol buscarPorCodigo(EntityManager em, String codigo) {
        return em.createQuery("SELECT r FROM Rol r ORDER BY r.idRol", Rol.class).getResultList().stream()
                .filter(r -> codigo != null && codigo.equals(r.getCodigo())).findFirst().orElse(null);
    }
}
