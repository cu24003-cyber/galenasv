package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** HU3: las proyecciones del dashboard usan parámetros, no traen el valor del paso y filtran por clínica. */
@ExtendWith(MockitoExtension.class)
class DashboardRepositoryTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Object[]> consulta;
    @InjectMocks DashboardRepository repositorio;
    private final UUID clinica = UUID.randomUUID(), id = UUID.randomUUID(), otro = UUID.randomUUID();
    private final OffsetDateTime desde = OffsetDateTime.parse("2026-11-13T06:00Z"), hasta = OffsetDateTime.parse("2026-11-14T06:00Z");

    private ArgumentCaptor<String> preparar() {
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        when(em.createQuery(jpql.capture(), eq(Object[].class))).thenReturn(consulta);
        lenient().when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(List.of());
        return jpql;
    }
    private static String select(String jpql) { return jpql.substring(0, jpql.indexOf(" FROM ")); }

    @Test void consultasFiltraPorClinicaYRangoConParametrosEnOrdenAscendente() {
        var jpql = preparar();
        assertTrue(repositorio.consultas(clinica, desde, hasta).isEmpty());
        String q = jpql.getValue();
        assertEquals("SELECT c.idConsulta, c.fechaInicio, c.fechaFin, per.idPersona, per.nombres, per.apellidos, per.fechaNacimiento", select(q));
        assertTrue(q.contains("pr.idClinica.idClinica=:clinica"));
        assertTrue(q.contains("c.fechaInicio>=:desde AND c.fechaInicio<:hasta"));
        assertTrue(q.endsWith("ORDER BY c.fechaInicio, c.idConsulta"));
        verify(consulta).setParameter("clinica", clinica);
        verify(consulta).setParameter("desde", desde);
        verify(consulta).setParameter("hasta", hasta);
        assertFalse(q.contains("observaciones") || q.contains("referenciaExterna"));
    }

    @Test void consultasSinClinicaNoConsulta() {
        assertTrue(repositorio.consultas(null, desde, hasta).isEmpty());
        verifyNoInteractions(em);
    }

    @Test void pasosNoSeleccionaElValorSoloSiExiste() {
        var jpql = preparar();
        repositorio.pasos(List.of(id, otro));
        String q = jpql.getValue();
        String columnas = select(q);
        assertTrue(columnas.contains("CASE WHEN p.valor IS NOT NULL THEN TRUE ELSE FALSE END"));
        assertFalse(columnas.replace("CASE WHEN p.valor IS NOT NULL THEN TRUE ELSE FALSE END", "").contains("valor"));
        assertFalse(columnas.contains("observaciones"));
        assertTrue(q.contains("LEFT JOIN cp.consultaProcedimientoPasoCollection p"));
        // B8: 18 quién completó o tiene asignado el paso y 19 si es de finalización (pendienteParaMi).
        assertTrue(columnas.endsWith("CASE WHEN p.valor IS NOT NULL THEN TRUE ELSE FALSE END, resp.idPersonaRol, def.indicaFin"), columnas);
        assertTrue(q.contains("cp.idConsulta.idConsulta IN :consultas"));
        verify(consulta).setParameter("consultas", List.of(id, otro));
        assertFalse(q.contains(id.toString()));
    }

    @Test void documentosNoSeleccionaLaRutaFisica() {
        var jpql = preparar();
        repositorio.documentos(List.of(id));
        String q = jpql.getValue();
        assertEquals("SELECT d.idPersona.idPersona, t.nombre, d.valor", select(q));
        assertTrue(q.contains("d.idPersona.idPersona IN :personas"));
        verify(consulta).setParameter("personas", List.of(id));
    }

    @Test void secuenciasDeLosProcedimientosIndicados() {
        var jpql = preparar();
        repositorio.secuencias(List.of(id));
        String q = jpql.getValue();
        assertEquals("SELECT s.idProcedimientoPaso.idProcedimientoPaso, s.idProcedimientoPasoReferencia.idProcedimientoPaso", select(q));
        assertTrue(q.contains("s.idProcedimientoPaso.idProcedimiento.idProcedimiento IN :procedimientos"));
        verify(consulta).setParameter("procedimientos", List.of(id));
    }

    @Test void listasVaciasONulasNoConsultan() {
        assertTrue(repositorio.pasos(List.of()).isEmpty());
        assertTrue(repositorio.documentos(null).isEmpty());
        assertTrue(repositorio.secuencias(List.of()).isEmpty());
        verifyNoInteractions(em);
    }

    @Test void valorPasoFiltraPorIdYClinicaEnElMismoWhere() {
        var jpql = preparar();
        assertTrue(repositorio.valorPaso(id, clinica).isEmpty());
        String q = jpql.getValue();
        assertEquals("SELECT p.idConsultaProcedimientoPaso, def.nombre, p.estado, p.fechaFin, p.valor", select(q));
        assertTrue(q.contains("p.idConsultaProcedimientoPaso=:id AND p.idConsultaProcedimiento.idConsulta.idPersonaRol.idClinica.idClinica=:clinica"));
        verify(consulta).setParameter("id", id);
        verify(consulta).setParameter("clinica", clinica);
    }

    // ---- B6: una sola consulta

    @Test void consultaFiltraPorIdYClinicaEnElMismoWhereConLasColumnasDelDashboardYSinFechas() {
        var jpql = preparar();
        assertTrue(repositorio.consulta(id, clinica).isEmpty());
        String q = jpql.getValue();
        verify(consulta).setParameter("id", id);
        verify(consulta).setParameter("clinica", clinica);
        assertTrue(q.contains("c.idConsulta=:id AND pr.idClinica.idClinica=:clinica"), q);
        assertFalse(q.contains("fechaInicio>=") || q.contains(":desde") || q.contains(":hasta"), q);
        reset(em);
        var rango = preparar();
        repositorio.consultas(clinica, desde, hasta);
        assertEquals(select(rango.getValue()), select(q));
        assertFalse(q.contains(id.toString()) || q.contains(clinica.toString()));
    }

    @Test void consultaSinIdOClinicaNoConsulta() {
        assertTrue(repositorio.consulta(null, clinica).isEmpty());
        assertTrue(repositorio.consulta(id, null).isEmpty());
        verifyNoInteractions(em);
    }

    @Test void valorPasoSinIdOClinicaNoConsulta() {
        assertTrue(repositorio.valorPaso(null, clinica).isEmpty());
        assertTrue(repositorio.valorPaso(id, null).isEmpty());
        verifyNoInteractions(em);
    }
}
