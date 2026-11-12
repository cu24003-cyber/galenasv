package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Usuario;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Usuario> consulta;
    @Mock TypedQuery<PersonaRol> consultaRoles;
    @InjectMocks UsuarioRepository repositorio;
    private final UUID tipo = UUID.randomUUID();

    @Test void yaNoExisteLaBusquedaPorUsuarioOCorreo() {
        assertTrue(java.util.Arrays.stream(UsuarioRepository.class.getDeclaredMethods()).noneMatch(m -> m.getName().equals("buscarAcceso")));
    }

    @Test void buscarPorDocumentoEnviaTipoYNumeroRecortadoComoParametros() {
        Usuario usuario = new Usuario();
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        when(em.createQuery(jpql.capture(), eq(Usuario.class))).thenReturn(consulta);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(consulta.getResultStream()).thenReturn(Stream.of(usuario));
        assertSame(usuario, repositorio.buscarPorDocumento(tipo, "  00000000-1 "));
        verify(consulta).setParameter("tipo", tipo);
        verify(consulta).setParameter("valor", "00000000-1");
        // El documento debe coincidir en tipo (activo) y valor; nada se concatena en la consulta.
        assertTrue(jpql.getValue().contains("d.idTipoDocumento.idTipoDocumento=:tipo"));
        assertTrue(jpql.getValue().contains("d.idTipoDocumento.activo=true"));
        assertTrue(jpql.getValue().contains("d.valor=:valor"));
        assertFalse(jpql.getValue().contains("00000000-1"));
    }

    @Test void buscarPorDocumentoDevuelveNullSiNoHayCoincidencia() {
        when(em.createQuery(anyString(), eq(Usuario.class))).thenReturn(consulta);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(consulta.getResultStream()).thenReturn(Stream.empty());
        assertNull(repositorio.buscarPorDocumento(tipo, "00000000-9"));
    }

    @Test void buscarPorDocumentoSinTipoONumeroNoConsulta() {
        assertNull(repositorio.buscarPorDocumento(null, "00000000-1"));
        assertNull(repositorio.buscarPorDocumento(tipo, "   "));
        assertNull(repositorio.buscarPorDocumento(tipo, null));
        verifyNoInteractions(em);
    }

    @Test void asignacionesSoloIncluyeRolesDeEmpleadoOrdenadasPorAntiguedad() {
        UUID persona = UUID.randomUUID();
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        when(em.createQuery(jpql.capture(), eq(PersonaRol.class))).thenReturn(consultaRoles);
        when(consultaRoles.setParameter("persona", persona)).thenReturn(consultaRoles);
        when(consultaRoles.getResultList()).thenReturn(List.of());
        repositorio.asignaciones(persona);
        assertTrue(jpql.getValue().contains("r.codigo IN ('ADMINISTRACION','DOCTOR')"));
        assertFalse(jpql.getValue().contains("PACIENTE"));
        assertTrue(jpql.getValue().contains("ORDER BY a.fechaCreacion, a.idPersonaRol"));
    }
}
