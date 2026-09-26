package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPaso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ProcedimientoPasoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcedimientoPasoServiceTest {

    @Mock
    private ProcedimientoPasoRepository procedimientoPasoRepository;

    @InjectMocks
    private ProcedimientoPasoService procedimientoPasoService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        ProcedimientoPaso esperada = new ProcedimientoPaso();
        
        when(procedimientoPasoRepository.find(id)).thenReturn(esperada);

        ProcedimientoPaso resultado = procedimientoPasoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(procedimientoPasoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(procedimientoPasoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoPasoService.eliminar(id));

        verify(procedimientoPasoRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        ProcedimientoPaso e = new ProcedimientoPaso();
        procedimientoPasoService.crear(e);
        assertNotNull(e.getIdProcedimientoPaso());
        verify(procedimientoPasoRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        ProcedimientoPaso e = new ProcedimientoPaso();
        e.setIdProcedimientoPaso(idFijo);
        procedimientoPasoService.crear(e);
        assertEquals(idFijo, e.getIdProcedimientoPaso());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        ProcedimientoPaso e = new ProcedimientoPaso();
        e.setIdProcedimientoPaso(id);
        when(procedimientoPasoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoPasoService.actualizar(e));
        verify(procedimientoPasoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        ProcedimientoPaso e = new ProcedimientoPaso();
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoRepository).create(any());

        assertThrows(ServiceException.class, () -> procedimientoPasoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ProcedimientoPaso e = new ProcedimientoPaso();
        e.setIdProcedimientoPaso(id);
        when(procedimientoPasoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoRepository).update(e);

        assertThrows(ServiceException.class, () -> procedimientoPasoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ProcedimientoPaso e = new ProcedimientoPaso();
        when(procedimientoPasoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoRepository).delete(id);

        assertThrows(ServiceException.class, () -> procedimientoPasoService.eliminar(id));
    }
}
