package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.MedioContacto;
import java.util.UUID;

@Stateless
public class MedioContactoRepository extends AbstractRepository<MedioContacto, UUID> {

    public MedioContactoRepository() {
        super(MedioContacto.class);
    }
    public java.util.List<MedioContacto> listarPorPersona(UUID idPersona) {
        return em.createQuery("SELECT e FROM MedioContacto e LEFT JOIN FETCH e.idTipoMedioContacto "
                + "WHERE e.idPersona.idPersona = :persona", MedioContacto.class)
                .setParameter("persona", idPersona).getResultList();
    }
}
