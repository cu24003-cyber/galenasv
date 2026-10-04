package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoMedioContacto;

@Stateless(name = "TipoMedioContactoRepository")
@LocalBean
public class TipoMedioContactoRepository extends AbstractRepository<TipoMedioContacto, UUID> {

    public TipoMedioContactoRepository() {
        super(TipoMedioContacto.class);
    }

    public boolean estaEnUso(UUID id) {
        return em.createQuery("SELECT COUNT(m) FROM MedioContacto m WHERE m.idTipoMedioContacto.idTipoMedioContacto=:id", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }
}
