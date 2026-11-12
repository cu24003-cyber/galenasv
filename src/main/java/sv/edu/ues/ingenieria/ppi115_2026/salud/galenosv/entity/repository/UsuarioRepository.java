package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Usuario;

@Stateless
public class UsuarioRepository extends AbstractRepository<Usuario, UUID> {
    public UsuarioRepository() { super(Usuario.class); }

    /** HU2: identifica al usuario por un documento cuyo tipo esté activo; el valor se compara exacto tras recortarlo. */
    public Usuario buscarPorDocumento(UUID idTipoDocumento, String numero) {
        if (idTipoDocumento == null || numero == null || numero.isBlank()) return null;
        return em.createQuery("SELECT u FROM Usuario u JOIN FETCH u.persona WHERE EXISTS ("
                + "SELECT d FROM Documento d WHERE d.idPersona=u.persona "
                + "AND d.idTipoDocumento.idTipoDocumento=:tipo AND d.idTipoDocumento.activo=true "
                + "AND d.valor=:valor)", Usuario.class)
                .setParameter("tipo", idTipoDocumento)
                .setParameter("valor", numero.strip())
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
                // HU2: solo los empleados se autentican; PACIENTE nunca es un rol de sesión.
                + "AND r.codigo IN ('ADMINISTRACION','DOCTOR') "
                + "ORDER BY a.fechaCreacion, a.idPersonaRol", PersonaRol.class)
                .setParameter("persona", idPersona).getResultList();
    }
}
