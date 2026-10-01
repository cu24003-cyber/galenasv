package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoExamenServiceTest {
    @Mock EntityManager em;
    @InjectMocks ExamenService servicio;

    private TipoExamen tipo(boolean activo) {
        TipoExamen tipo = new TipoExamen(UUID.randomUUID()); tipo.setActivo(activo);
        when(em.find(TipoExamen.class, tipo.getIdTipoExamen())).thenReturn(tipo); return tipo;
    }
    private Examen examen() { Examen e = new Examen(); e.setNombre(" Hemograma "); e.setActivo(true); return e; }

    @Test void creaExamenYClasificacionesSinDuplicados() {
        Examen datos = examen(); TipoExamen tipo = tipo(true);
        servicio.guardarConTipos(datos, List.of(tipo.getIdTipoExamen(), tipo.getIdTipoExamen()));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class); verify(em, times(2)).persist(captor.capture());
        Examen guardado = (Examen) captor.getAllValues().get(0);
        ExamenTipoExamen enlace = (ExamenTipoExamen) captor.getAllValues().get(1);
        assertEquals("Hemograma", guardado.getNombre()); assertSame(guardado, enlace.getIdExamen());
        assertSame(tipo, enlace.getIdTipoExamen()); assertNotNull(enlace.getFechaCreacion());
        assertNull(datos.getIdExamen(), "El formulario conserva la posibilidad de reintentar una creación fallida");
        verify(em).flush();
    }
    @Test void rechazaTipoInactivoAntesDeGuardar() {
        TipoExamen tipo = tipo(false);
        assertThrows(ServiceException.class, () -> servicio.guardarConTipos(examen(), List.of(tipo.getIdTipoExamen())));
        verify(em, never()).persist(any());
    }
    @Test void rechazaNombreVacioYClasificacionVacia() {
        Examen datos = examen(); datos.setNombre(" ");
        assertThrows(ServiceException.class, () -> servicio.guardarConTipos(datos, List.of(UUID.randomUUID())));
        datos.setNombre("Hemograma");
        assertThrows(ServiceException.class, () -> servicio.guardarConTipos(datos, List.of()));
        verifyNoInteractions(em);
    }
    @SuppressWarnings("unchecked")
    @Test void edicionConservaFechaDeClasificacionExistenteInclusoInactiva() {
        Examen datos = examen(); datos.setIdExamen(UUID.randomUUID()); Examen guardado = new Examen(datos.getIdExamen());
        when(em.find(Examen.class, datos.getIdExamen(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(guardado);
        TipoExamen conservado = tipo(false); TipoExamen agregado = tipo(true);
        TipoExamen retirado = new TipoExamen(UUID.randomUUID());
        ExamenTipoExamen anterior = new ExamenTipoExamen(UUID.randomUUID()); anterior.setIdTipoExamen(conservado);
        OffsetDateTime fecha = OffsetDateTime.parse("2026-09-29T10:00:00Z"); anterior.setFechaCreacion(fecha);
        ExamenTipoExamen quitar = new ExamenTipoExamen(UUID.randomUUID()); quitar.setIdTipoExamen(retirado);
        TypedQuery<ExamenTipoExamen> q = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(ExamenTipoExamen.class))).thenReturn(q);
        when(q.setParameter("examen", guardado)).thenReturn(q); when(q.getResultList()).thenReturn(List.of(anterior, quitar));
        servicio.guardarConTipos(datos, List.of(conservado.getIdTipoExamen(), agregado.getIdTipoExamen()));
        verify(em).remove(quitar); verify(em, never()).remove(anterior);
        ArgumentCaptor<ExamenTipoExamen> enlace = ArgumentCaptor.forClass(ExamenTipoExamen.class);
        verify(em).persist(enlace.capture()); assertSame(agregado, enlace.getValue().getIdTipoExamen());
        assertEquals(fecha, anterior.getFechaCreacion()); assertEquals("Hemograma", guardado.getNombre());
    }
    @SuppressWarnings("unchecked")
    @Test void noEliminaExamenUsadoEnProcedimientos() {
        Examen examen = new Examen(UUID.randomUUID());
        when(em.find(Examen.class, examen.getIdExamen(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(examen);
        TypedQuery<Long> q = mock(TypedQuery.class); when(em.createQuery(anyString(), eq(Long.class))).thenReturn(q);
        when(q.setParameter("examen", examen)).thenReturn(q); when(q.getSingleResult()).thenReturn(1L);
        assertThrows(ServiceException.class, () -> servicio.eliminarConTipos(examen.getIdExamen()));
        verify(em, never()).remove(any()); verify(em, never()).createQuery(anyString());
    }
}
