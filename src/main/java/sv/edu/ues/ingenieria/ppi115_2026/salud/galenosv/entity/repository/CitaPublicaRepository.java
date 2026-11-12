package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * HU1: consultas sin sesión. Devuelven proyecciones (nunca entidades) con solo los campos
 * públicos, y siempre filtran por el documento en la misma consulta: no hay un paso previo
 * que distinga "documento inexistente" de "documento sin datos".
 */
@Stateless
public class CitaPublicaRepository {
    @PersistenceContext EntityManager em;

    private static final String PACIENTE_CON_DOCUMENTO =
            "r.codigo='PACIENTE' AND r.activo=true AND c.activo=true AND EXISTS ("
            + "SELECT d FROM Documento d WHERE d.idPersona=a.idPersona "
            + "AND d.idTipoDocumento.idTipoDocumento=:tipo AND d.idTipoDocumento.activo=true AND d.valor=:valor)";

    /** [idClinica, nombre] de las clínicas activas donde el dueño del documento es PACIENTE. */
    public List<Object[]> clinicas(UUID tipo, String numero) {
        if (invalido(tipo, numero)) return List.of();
        return em.createQuery("SELECT DISTINCT c.idClinica, c.nombre FROM PersonaRol a JOIN a.idClinica c JOIN a.idRol r WHERE "
                + PACIENTE_CON_DOCUMENTO + " ORDER BY c.nombre, c.idClinica", Object[].class)
                .setParameter("tipo", tipo).setParameter("valor", numero.strip())
                .getResultList();
    }

    /** [idConsulta, fechaInicio, nombreClinica] de las consultas del documento en esa clínica, desde ≤ fecha < hasta. */
    public List<Object[]> citas(UUID tipo, String numero, UUID clinica, OffsetDateTime desde, OffsetDateTime hasta) {
        if (invalido(tipo, numero) || clinica == null || desde == null || hasta == null) return List.of();
        return em.createQuery(BASE_CITA + " AND c.idClinica=:clinica AND con.fechaInicio>=:desde AND con.fechaInicio<:hasta"
                + " ORDER BY con.fechaInicio, con.idConsulta", Object[].class)
                .setParameter("tipo", tipo).setParameter("valor", numero.strip()).setParameter("clinica", clinica)
                .setParameter("desde", desde).setParameter("hasta", hasta)
                .getResultList();
    }

    /** La consulta indicada, solo si pertenece al documento (para el ticket). */
    public List<Object[]> cita(UUID tipo, String numero, UUID idConsulta) {
        if (invalido(tipo, numero) || idConsulta == null) return List.of();
        return em.createQuery(BASE_CITA + " AND con.idConsulta=:consulta", Object[].class)
                .setParameter("tipo", tipo).setParameter("valor", numero.strip()).setParameter("consulta", idConsulta)
                .getResultList();
    }

    /** [idConsulta, nombreProcedimiento] en orden de inicio. */
    public List<Object[]> procedimientos(List<UUID> consultas) {
        if (consultas == null || consultas.isEmpty()) return List.of();
        return em.createQuery("SELECT cp.idConsulta.idConsulta, p.nombre FROM ConsultaProcedimiento cp JOIN cp.idProcedimiento p "
                + "WHERE cp.idConsulta.idConsulta IN :consultas ORDER BY cp.fechaInicio, cp.idConsultaProcedimiento", Object[].class)
                .setParameter("consultas", consultas)
                .getResultList();
    }

    private static final String BASE_CITA = "SELECT con.idConsulta, con.fechaInicio, c.nombre FROM Consulta con "
            + "JOIN con.idPersonaRol a JOIN a.idClinica c JOIN a.idRol r WHERE " + PACIENTE_CON_DOCUMENTO;

    private static boolean invalido(UUID tipo, String numero) {
        return tipo == null || numero == null || numero.isBlank();
    }
}
