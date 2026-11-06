package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Usuario;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Usuario> consulta;
    @InjectMocks UsuarioRepository repositorio;

    @Test void usuarioOCorreoSeNormalizanYSeEnvianComoParametro() {
        Usuario usuario = new Usuario();
        when(em.createQuery(anyString(), eq(Usuario.class))).thenReturn(consulta);
        when(consulta.setParameter("identificador", "julio@example.invalid")).thenReturn(consulta);
        when(consulta.getResultStream()).thenReturn(Stream.of(usuario));
        assertSame(usuario, repositorio.buscarAcceso("  JULIO@EXAMPLE.INVALID  "));
        verify(consulta).setParameter("identificador", "julio@example.invalid");
    }
}
