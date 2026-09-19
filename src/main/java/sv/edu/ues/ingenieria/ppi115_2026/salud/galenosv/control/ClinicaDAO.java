package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import java.util.UUID;

@Stateless
public class ClinicaDAO extends DefaultDAO<Clinica, UUID> {

    public ClinicaDAO() {
        super(Clinica.class);
    }
}
