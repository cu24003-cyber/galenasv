package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ProcedimientoPasoExamenRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcedimientoPasoExamenServiceTest {

    @Mock
    private ProcedimientoPasoExamenRepository procedimientoPasoExamenRepository;

    @InjectMocks
    private ProcedimientoPasoExamenService procedimientoPasoExamenService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        ProcedimientoPasoExamen esperada = new ProcedimientoPasoExamen();
        
        when(procedimientoPasoExamenRepository.find(id)).thenReturn(esperada);

        ProcedimientoPasoExamen resultado = procedimientoPasoExamenService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(procedimientoPasoExamenRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(procedimientoPasoExamenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoPasoExamenService.eliminar(id));

        verify(procedimientoPasoExamenRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        ProcedimientoPasoExamen e = new ProcedimientoPasoExamen();
        procedimientoPasoExamenService.crear(e);
        assertNotNull(e.getIdProcedimientoPasoExamen());
        verify(procedimientoPasoExamenRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        ProcedimientoPasoExamen e = new ProcedimientoPasoExamen();
        e.setIdProcedimientoPasoExamen(idFijo);
        procedimientoPasoExamenService.crear(e);
        assertEquals(idFijo, e.getIdProcedimientoPasoExamen());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        ProcedimientoPasoExamen e = new ProcedimientoPasoExamen();
        e.setIdProcedimientoPasoExamen(id);
        when(procedimientoPasoExamenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoPasoExamenService.actualizar(e));
        verify(procedimientoPasoExamenRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        ProcedimientoPasoExamen e = new ProcedimientoPasoExamen();
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoExamenRepository).create(any());

        assertThrows(ServiceException.class, () -> procedimientoPasoExamenService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ProcedimientoPasoExamen e = new ProcedimientoPasoExamen();
        e.setIdProcedimientoPasoExamen(id);
        when(procedimientoPasoExamenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoExamenRepository).update(e);

        assertThrows(ServiceException.class, () -> procedimientoPasoExamenService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ProcedimientoPasoExamen e = new ProcedimientoPasoExamen();
        when(procedimientoPasoExamenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoExamenRepository).delete(id);

        assertThrows(ServiceException.class, () -> procedimientoPasoExamenService.eliminar(id));
    }
}
