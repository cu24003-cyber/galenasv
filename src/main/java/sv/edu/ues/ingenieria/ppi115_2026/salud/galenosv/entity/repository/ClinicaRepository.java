package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

@Stateless
public class ClinicaRepository extends AbstractRepository<Clinica, UUID> {

    public List<Clinica> findClinicaByName(String nameClinica){
        if(nameClinica==null||nameClinica.isBlank()){
            return findAll();
        }
        return em.createQuery("SELECT c FROM Clinica c WHERE LOCATE(:nameClinica, LOWER(c.nombre)) > 0 "
                + "ORDER BY c.nombre", Clinica.class).
                setParameter("nameClinica", nameClinica.strip().toLowerCase(Locale.ROOT))
                .getResultList();
    }

    public ClinicaRepository() {
        super(Clinica.class);
    }
}
