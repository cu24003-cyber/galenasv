package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

@ExtendWith(MockitoExtension.class)
class ContextoRolServiceTest {
    @Mock AccesoSesion acceso;
    @Mock AutenticacionService autenticacion;
    @InjectMocks ContextoRolService servicio;

    @Test void noListaPersonasCuandoNoHayUnaCuentaAutenticada() {
        assertTrue(servicio.listar().isEmpty()); verifyNoInteractions(autenticacion);
    }
    @Test void soloListaLaCuentaActualYRechazaUuidAjenos() {
        UUID usuario = UUID.randomUUID(); PersonaRol propia = new PersonaRol(UUID.randomUUID());
        when(acceso.vigente()).thenReturn(true); when(acceso.getIdUsuario()).thenReturn(usuario);
        when(autenticacion.asignaciones(usuario)).thenReturn(List.of(propia));
        assertSame(propia, servicio.cargar(propia.getIdPersonaRol()));
        assertThrows(ServiceException.class, () -> servicio.cargar(UUID.randomUUID()));
        verify(autenticacion, times(2)).asignaciones(usuario);
    }
}
