package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

@ExtendWith(MockitoExtension.class)
class PersonaRepositoryTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Persona> query;
    @InjectMocks PersonaRepository repository;

    @Test
    void buscarPorNombre_normalizaYEnviaElTextoComoParametroLiteral() {
        List<Persona> personas = List.of(new Persona());
        when(em.createQuery(anyString(), eq(Persona.class))).thenReturn(query);
        when(query.setParameter("nombre", "ana%_'" )).thenReturn(query);
        when(query.getResultList()).thenReturn(personas);

        assertSame(personas, repository.buscarPorNombre("  ANA%_'  "));

        verify(em).createQuery(contains("LOCATE(:nombre, LOWER(p.nombres)) > 0"), eq(Persona.class));
        verify(query).setParameter("nombre", "ana%_'");
    }

    @Test
    void buscarPorNombre_vacioONuloDevuelveTodos() {
        when(em.createQuery("SELECT e FROM Persona e", Persona.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertTrue(repository.buscarPorNombre(null).isEmpty());
        assertTrue(repository.buscarPorNombre("   ").isEmpty());

        verify(query, never()).setParameter(anyString(), any());
    }
}
