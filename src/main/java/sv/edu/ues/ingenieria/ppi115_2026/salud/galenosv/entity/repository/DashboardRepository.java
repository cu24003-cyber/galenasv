package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * HU3: proyecciones del dashboard. Cada método es una sola consulta para todas las consultas
 * del rango (sin N+1), con parámetros y sin traer el valor de los pasos, salvo valorPaso.
 */
@Stateless
public class DashboardRepository {
    @PersistenceContext EntityManager em;

    /** Columnas de la consulta y su paciente; las comparten consultas() y consulta(). */
    private static final String CONSULTA = "SELECT c.idConsulta, c.fechaInicio, c.fechaFin, per.idPersona, per.nombres, per.apellidos, per.fechaNacimiento"
            + " FROM Consulta c JOIN c.idPersonaRol pr JOIN pr.idPersona per";

    /**
     * [idConsulta, fechaInicio, fechaFin, idPersona, nombres, apellidos, fechaNacimiento] de las
     * consultas de la clínica con desde ≤ fechaInicio < hasta.
     */
    public List<Object[]> consultas(UUID clinica, OffsetDateTime desde, OffsetDateTime hasta) {
        if (clinica == null || desde == null || hasta == null) return List.of();
        return em.createQuery(CONSULTA
                + " WHERE pr.idClinica.idClinica=:clinica AND c.fechaInicio>=:desde AND c.fechaInicio<:hasta"
                + " ORDER BY c.fechaInicio, c.idConsulta", Object[].class)
                .setParameter("clinica", clinica).setParameter("desde", desde).setParameter("hasta", hasta)
                .getResultList();
    }

    /**
     * Procedimientos y sus pasos. Un procedimiento sin pasos aparece una vez con las columnas
     * del paso en null. Columnas:
     * 0 idConsulta, 1 idConsultaProcedimiento, 2 idProcedimiento, 3 nombre del procedimiento,
     * 4 fechaInicio y 5 fechaFin del procedimiento, 6 idConsultaProcedimientoPaso,
     * 7 idProcedimientoPaso (definición), 8 nombre del paso, 9 estado, 10 fechaInicio, 11 fechaFin,
     * 12 nombres y 13 apellidos del responsable, 14 nombre del rol del responsable,
     * 15 idRol de la definición, 16 idRol del responsable, 17 tieneValor,
     * 18 idPersonaRol del responsable (quien lo completó), 19 indicaFin de la definición y
     * 20 nombre del rol de la definición (B8).
     */
    public List<Object[]> pasos(List<UUID> consultas) {
        if (consultas == null || consultas.isEmpty()) return List.of();
        return em.createQuery("SELECT cp.idConsulta.idConsulta, cp.idConsultaProcedimiento, proc.idProcedimiento, proc.nombre,"
                + " cp.fechaInicio, cp.fechaFin, p.idConsultaProcedimientoPaso, def.idProcedimientoPaso, def.nombre,"
                + " p.estado, p.fechaInicio, p.fechaFin, per.nombres, per.apellidos, rolResp.nombre,"
                + " rolDef.idRol, rolResp.idRol, CASE WHEN p.valor IS NOT NULL THEN TRUE ELSE FALSE END, resp.idPersonaRol, def.indicaFin, rolDef.nombre"
                + " FROM ConsultaProcedimiento cp JOIN cp.idProcedimiento proc"
                + " LEFT JOIN cp.consultaProcedimientoPasoCollection p LEFT JOIN p.idProcedimientoPaso def"
                + " LEFT JOIN def.idRol rolDef LEFT JOIN p.idPersonaRol resp LEFT JOIN resp.idPersona per"
                + " LEFT JOIN resp.idRol rolResp"
                + " WHERE cp.idConsulta.idConsulta IN :consultas"
                + " ORDER BY cp.fechaInicio, cp.idConsultaProcedimiento, p.fechaInicio, p.idConsultaProcedimientoPaso", Object[].class)
                .setParameter("consultas", consultas)
                .getResultList();
    }

    /**
     * B6: las mismas columnas que consultas() para una consulta, solo si es de esa clínica y sin
     * rango de fechas. Un id inexistente y uno de otra clínica dan la misma lista vacía.
     */
    public List<Object[]> consulta(UUID id, UUID clinica) {
        if (id == null || clinica == null) return List.of();
        return em.createQuery(CONSULTA + " WHERE c.idConsulta=:id AND pr.idClinica.idClinica=:clinica", Object[].class)
                .setParameter("id", id).setParameter("clinica", clinica)
                .getResultList();
    }

    /** [idPersona, nombre del tipo, número] de los documentos de esas personas. */
    public List<Object[]> documentos(List<UUID> personas) {
        if (personas == null || personas.isEmpty()) return List.of();
        return em.createQuery("SELECT d.idPersona.idPersona, t.nombre, d.valor FROM Documento d JOIN d.idTipoDocumento t"
                + " WHERE d.idPersona.idPersona IN :personas ORDER BY t.nombre, d.valor", Object[].class)
                .setParameter("personas", personas)
                .getResultList();
    }

    /** [paso origen, paso destino] del catálogo de esos procedimientos. */
    public List<Object[]> secuencias(List<UUID> procedimientos) {
        if (procedimientos == null || procedimientos.isEmpty()) return List.of();
        return em.createQuery("SELECT s.idProcedimientoPaso.idProcedimientoPaso, s.idProcedimientoPasoReferencia.idProcedimientoPaso"
                + " FROM ProcedimientoPasoSecuencia s"
                + " WHERE s.idProcedimientoPaso.idProcedimiento.idProcedimiento IN :procedimientos", Object[].class)
                .setParameter("procedimientos", procedimientos)
                .getResultList();
    }

    /**
     * [id, nombre del paso, estado, fechaFin, valor] solo si el paso es de una consulta de esa
     * clínica. Un id inexistente y uno de otra clínica dan la misma lista vacía.
     */
    public List<Object[]> valorPaso(UUID id, UUID clinica) {
        if (id == null || clinica == null) return List.of();
        return em.createQuery("SELECT p.idConsultaProcedimientoPaso, def.nombre, p.estado, p.fechaFin, p.valor"
                + " FROM ConsultaProcedimientoPaso p LEFT JOIN p.idProcedimientoPaso def"
                + " WHERE p.idConsultaProcedimientoPaso=:id AND p.idConsultaProcedimiento.idConsulta.idPersonaRol.idClinica.idClinica=:clinica", Object[].class)
                .setParameter("id", id).setParameter("clinica", clinica)
                .getResultList();
    }
}
