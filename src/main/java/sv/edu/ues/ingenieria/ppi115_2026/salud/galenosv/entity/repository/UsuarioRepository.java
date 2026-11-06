package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Usuario;

@Stateless
public class UsuarioRepository extends AbstractRepository<Usuario, UUID> {
    public UsuarioRepository() { super(Usuario.class); }

    public Usuario buscarAcceso(String identificador) {
        return em.createQuery("SELECT u FROM Usuario u JOIN FETCH u.persona WHERE "
                + "LOWER(u.nombreUsuario)=:identificador OR LOWER(u.correo)=:identificador", Usuario.class)
                .setParameter("identificador", identificador.strip().toLowerCase(Locale.ROOT))
                .getResultStream().findFirst().orElse(null);
    }

    public Usuario buscarSesion(UUID idUsuario) {
        return em.createQuery("SELECT u FROM Usuario u JOIN FETCH u.persona WHERE u.idUsuario=:id", Usuario.class)
                .setParameter("id", idUsuario).getResultStream().findFirst().orElse(null);
    }

    public List<PersonaRol> asignaciones(UUID idPersona) {
        return em.createQuery("SELECT a FROM PersonaRol a JOIN FETCH a.idPersona p "
                + "JOIN FETCH a.idRol r JOIN FETCH a.idClinica c WHERE p.idPersona=:persona "
                + "AND r.activo=true AND c.activo=true "
                + "AND r.codigo IN ('ADMINISTRACION','DOCTOR','PACIENTE') "
                + "ORDER BY a.fechaCreacion, a.idPersonaRol", PersonaRol.class)
                .setParameter("persona", idPersona).getResultList();
    }
}
