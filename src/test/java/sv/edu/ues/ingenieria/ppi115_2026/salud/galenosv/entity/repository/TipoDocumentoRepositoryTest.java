package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

@ExtendWith(MockitoExtension.class)
class TipoDocumentoRepositoryTest {
    @Mock EntityManager em;
    @Mock TypedQuery<TipoDocumento> consulta;
    @InjectMocks TipoDocumentoRepository repositorio;

    @Test void listarActivosFiltraPorActivoYOrdenaPorNombre() {
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        when(em.createQuery(jpql.capture(), eq(TipoDocumento.class))).thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(List.of());
        assertTrue(repositorio.listarActivos().isEmpty());
        assertTrue(jpql.getValue().contains("t.activo=true"));
        assertTrue(jpql.getValue().endsWith("ORDER BY t.nombre"));
    }
}
