package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.persistence.*;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ContenidoOrden;

@ExtendWith(MockitoExtension.class)
class CompatibilidadOrdenesServiceTest {
    @Mock EntityManager em;
    @Mock Query columnas;
    @Mock Query ordenes;
    @InjectMocks CompatibilidadOrdenesService servicio;

    private void columna(long cantidad) {
        when(em.createNativeQuery(startsWith("SELECT COUNT(*)"))).thenReturn(columnas);
        when(columnas.getSingleResult()).thenReturn(cantidad);
    }
    @Test void baseOriginalNoConsultaUnaColumnaQueNoExiste() {
        columna(0); servicio.verificar();
        verify(em, times(1)).createNativeQuery(anyString());
    }
    @Test void exigeConversionAntesDeIgnorarAsociacionesAntiguas() {
        columna(1);
        when(em.createNativeQuery(startsWith("SELECT indicaciones"))).thenReturn(ordenes);
        when(ordenes.getResultStream()).thenReturn(Stream.of("En ayunas"));
        assertThrows(IllegalStateException.class, servicio::verificar);
    }
    @Test void admiteBaseConvertidaIncluidoUnExamenRetiradoDespues() {
        columna(1);
        when(em.createNativeQuery(startsWith("SELECT indicaciones"))).thenReturn(ordenes);
        when(ordenes.getResultStream()).thenReturn(Stream.of(ContenidoOrden.guardar(UUID.randomUUID(), null),
                ContenidoOrden.guardar(null, "Texto editado")));
        assertDoesNotThrow(servicio::verificar);
    }
}
