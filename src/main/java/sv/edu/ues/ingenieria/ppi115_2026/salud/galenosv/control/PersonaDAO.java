package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import java.util.UUID;

@Stateless
public class PersonaDAO extends DefaultDAO<Persona, UUID> {

    public PersonaDAO() {
        super(Persona.class);
    }
}