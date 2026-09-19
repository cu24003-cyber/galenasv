package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.MedioContacto;
import java.util.UUID;

@Stateless
public class MedioContactoDAO extends DefaultDAO<MedioContacto, UUID> {

    public MedioContactoDAO() {
        super(MedioContacto.class);
    }
}
