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

@ExtendWith(MockitoExtension.class)
class CitaPublicaRepositoryTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Object[]> consulta;
    @InjectMocks CitaPublicaRepository repositorio;
    private final UUID tipo = UUID.randomUUID(), clinica = UUID.randomUUID();
    private final OffsetDateTime desde = OffsetDateTime.parse("2026-11-01T06:00Z"), hasta = OffsetDateTime.parse("2026-12-01T06:00Z");

    private ArgumentCaptor<String> preparar() {
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        when(em.createQuery(jpql.capture(), eq(Object[].class))).thenReturn(consulta);
        lenient().when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(List.of());
        return jpql;
    }
    private static String select(String jpql) { return jpql.substring(0, jpql.indexOf(" FROM ")); }
    private static void filtraDocumentoYPaciente(String jpql) {
        assertTrue(jpql.contains("d.idTipoDocumento.idTipoDocumento=:tipo"));
        assertTrue(jpql.contains("d.idTipoDocumento.activo=true"));
        assertTrue(jpql.contains("d.valor=:valor"));
        assertTrue(jpql.contains("r.codigo='PACIENTE'"));
        assertTrue(jpql.contains("c.activo=true"));
    }

    @Test void clinicasFiltraPorDocumentoYRolPacienteConParametros() {
        var jpql = preparar();
        assertTrue(repositorio.clinicas(tipo, " 00000000-3 ").isEmpty());
        filtraDocumentoYPaciente(jpql.getValue());
        assertEquals("SELECT DISTINCT c.idClinica, c.nombre", select(jpql.getValue()));
        verify(consulta).setParameter("tipo", tipo); verify(consulta).setParameter("valor", "00000000-3");
        assertFalse(jpql.getValue().contains("00000000-3"));
    }

    @Test void citasSoloSeleccionaIdFechaYClinica() {
        var jpql = preparar();
        repositorio.citas(tipo, "00000000-3", clinica, desde, hasta);
        filtraDocumentoYPaciente(jpql.getValue());
        assertEquals("SELECT con.idConsulta, con.fechaInicio, c.nombre", select(jpql.getValue()));
        assertTrue(jpql.getValue().contains("c.idClinica=:clinica"));
        assertTrue(jpql.getValue().contains("con.fechaInicio>=:desde AND con.fechaInicio<:hasta"));
        assertTrue(jpql.getValue().endsWith("ORDER BY con.fechaInicio, con.idConsulta"));
        verify(consulta).setParameter("clinica", clinica);
        verify(consulta).setParameter("desde", desde); verify(consulta).setParameter("hasta", hasta);
    }

    @Test void citaPorIdTambienExigeQueSeaDelDocumento() {
        var jpql = preparar(); UUID id = UUID.randomUUID();
        assertTrue(repositorio.cita(tipo, "00000000-3", id).isEmpty());
        filtraDocumentoYPaciente(jpql.getValue());
        assertTrue(jpql.getValue().contains("con.idConsulta=:consulta"));
        verify(consulta).setParameter("consulta", id);
    }

    @Test void procedimientosSoloDevuelveNombres() {
        var jpql = preparar(); UUID id = UUID.randomUUID();
        repositorio.procedimientos(List.of(id));
        assertEquals("SELECT cp.idConsulta.idConsulta, p.nombre", select(jpql.getValue()));
        verify(consulta).setParameter("consultas", List.of(id));
    }

    @Test void entradasVaciasNoConsultanLaBase() {
        assertTrue(repositorio.clinicas(null, "x").isEmpty());
        assertTrue(repositorio.clinicas(tipo, " ").isEmpty());
        assertTrue(repositorio.citas(tipo, null, clinica, desde, hasta).isEmpty());
        assertTrue(repositorio.cita(tipo, "x", null).isEmpty());
        assertTrue(repositorio.procedimientos(List.of()).isEmpty());
        verifyNoInteractions(em);
    }

    @Test void ningunaConsultaSeleccionaDatosClinicos() {
        var jpql = preparar();
        repositorio.clinicas(tipo, "a"); repositorio.citas(tipo, "a", clinica, desde, hasta);
        repositorio.cita(tipo, "a", UUID.randomUUID()); repositorio.procedimientos(List.of(UUID.randomUUID()));
        for (String q : jpql.getAllValues()) {
            String s = select(q).toLowerCase();
            for (String prohibido : List.of("observaciones", "valor", "referencia", "estado", "personarol", "resultado", "persona"))
                assertFalse(s.contains(prohibido), q);
        }
    }
}
