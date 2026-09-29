package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
