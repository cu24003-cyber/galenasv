package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import java.util.UUID;

@Stateless
public class PersonaRolDAO extends DefaultDAO<PersonaRol, UUID> {

    public PersonaRolDAO() {
        super(PersonaRol.class);
    }
}
