package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import java.util.UUID;

@Stateless
public class ClinicaRepository extends AbstractRepository<Clinica, UUID> {

    public ClinicaRepository() {
        super(Clinica.class);
    }
}
