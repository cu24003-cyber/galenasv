package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import java.util.UUID;

@Stateless
public class TipoMedioContactoDAO extends DefaultDAO<TipoMedioContacto, UUID> {

    public TipoMedioContactoDAO() {
        super(TipoMedioContacto.class);
    }
}
