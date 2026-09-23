package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import java.util.UUID;

@Stateless
public class PersonaRepository extends AbstractRepository<Persona, UUID> {

    public PersonaRepository() {
        super(Persona.class);
    }
}