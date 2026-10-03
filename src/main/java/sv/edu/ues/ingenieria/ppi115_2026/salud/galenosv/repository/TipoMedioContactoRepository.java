package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import jakarta.ejb.LocalBean;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import java.util.UUID;

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
