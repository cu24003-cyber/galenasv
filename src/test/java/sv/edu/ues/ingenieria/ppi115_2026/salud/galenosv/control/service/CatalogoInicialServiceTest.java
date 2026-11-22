package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.UUID;
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
    @Mock TypedQuery<Rol> query;
    @InjectMocks CatalogoInicialService servicio;

    private void existentes(String... nombres) {
        when(em.createQuery(anyString(), eq(Rol.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.stream(nombres).map(n -> {
            Rol rol = new Rol(UUID.randomUUID()); rol.setNombre(n); return rol;
        }).toList());
    }

    @Test
    void creaPacienteActivoSiNoExiste() {
        existentes("Doctor", "Atención al público", "Paciente externo");
        servicio.inicializar();
        var captor = org.mockito.ArgumentCaptor.forClass(Rol.class);
        verify(em).persist(captor.capture());
        assertEquals("Paciente", captor.getValue().getNombre());
        assertTrue(captor.getValue().getActivo());
        assertNotNull(captor.getValue().getIdRol());
    }

    @Test
    void respetaRolExistenteSinDuplicarlo() {
        existentes("Paciente");
        servicio.inicializar();
        verify(em, never()).persist(any());
    }

    @Test
    void reconoceElPacienteCreadoPorElUsuarioConOtraEscritura() {
        for (String nombre : new String[]{" PACIENTE ", "paciente", "Pacienté"}) {
            existentes("Doctor", nombre);
            servicio.inicializar();
        }
        verify(em, never()).persist(any());
    }
}
