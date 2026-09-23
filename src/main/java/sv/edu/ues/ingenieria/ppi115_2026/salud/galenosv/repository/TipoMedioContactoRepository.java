package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import java.util.UUID;

@Stateless
public class TipoMedioContactoRepository extends AbstractRepository<TipoMedioContacto, UUID> {

    public TipoMedioContactoRepository() {
        super(TipoMedioContacto.class);
    }
}
