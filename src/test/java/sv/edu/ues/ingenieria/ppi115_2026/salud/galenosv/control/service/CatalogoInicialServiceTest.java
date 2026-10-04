package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

@ExtendWith(MockitoExtension.class)
class CatalogoInicialServiceTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Long> query;
    @InjectMocks CatalogoInicialService servicio;

    private void existentes(long cantidad) {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter("nombre", "paciente")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(cantidad);
    }

    @Test
    void creaPacienteActivoSiNoExiste() {
        existentes(0);
        servicio.inicializar();
        var captor = org.mockito.ArgumentCaptor.forClass(Rol.class);
        verify(em).persist(captor.capture());
        assertEquals("Paciente", captor.getValue().getNombre());
        assertTrue(captor.getValue().getActivo());
        assertNotNull(captor.getValue().getIdRol());
    }

    @Test
    void respetaRolExistenteSinDuplicarlo() {
        existentes(1);
        servicio.inicializar();
        verify(em, never()).persist(any());
    }
}
