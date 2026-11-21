package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

@ExtendWith(MockitoExtension.class)
class DocumentoRepositoryTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Persona> consulta;
    @InjectMocks DocumentoRepository repositorio;

    @Test void soloIdentificaUnaPersonaSiElDocumentoNoEsAmbiguo() {
        UUID tipo = UUID.randomUUID();
        Persona primera = new Persona(UUID.randomUUID()), segunda = new Persona(UUID.randomUUID());
        when(em.createQuery(startsWith("SELECT DISTINCT p FROM Documento"), eq(Persona.class))).thenReturn(consulta);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(consulta.setMaxResults(2)).thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(List.of(), List.of(primera), List.of(primera, segunda));
        assertNull(repositorio.buscarPersonaPorDocumento(tipo, " ficticio "));
        assertSame(primera, repositorio.buscarPersonaPorDocumento(tipo, " ficticio "));
        assertNull(repositorio.buscarPersonaPorDocumento(tipo, " ficticio "));
        verify(consulta, times(3)).setParameter("valor", "ficticio");
    }

    @Test void noConsultaSiFaltaElDocumento() {
        assertNull(repositorio.buscarPersonaPorDocumento(null, "ficticio"));
        assertNull(repositorio.buscarPersonaPorDocumento(UUID.randomUUID(), " "));
        verifyNoInteractions(em);
    }
}
