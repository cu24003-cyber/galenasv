package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoSecuencia;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ProcedimientoPasoSecuenciaRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcedimientoPasoSecuenciaServiceTest {

    @Mock
    private ProcedimientoPasoSecuenciaRepository procedimientoPasoSecuenciaRepository;

    @InjectMocks
    private ProcedimientoPasoSecuenciaService procedimientoPasoSecuenciaService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        ProcedimientoPasoSecuencia esperada = new ProcedimientoPasoSecuencia();
        
        when(procedimientoPasoSecuenciaRepository.find(id)).thenReturn(esperada);

        ProcedimientoPasoSecuencia resultado = procedimientoPasoSecuenciaService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(procedimientoPasoSecuenciaRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(procedimientoPasoSecuenciaRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoPasoSecuenciaService.eliminar(id));

        verify(procedimientoPasoSecuenciaRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        ProcedimientoPasoSecuencia e = new ProcedimientoPasoSecuencia();
        procedimientoPasoSecuenciaService.crear(e);
        assertNotNull(e.getIdProcedimientoPasoSecuencia());
        verify(procedimientoPasoSecuenciaRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        ProcedimientoPasoSecuencia e = new ProcedimientoPasoSecuencia();
        e.setIdProcedimientoPasoSecuencia(idFijo);
        procedimientoPasoSecuenciaService.crear(e);
        assertEquals(idFijo, e.getIdProcedimientoPasoSecuencia());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        ProcedimientoPasoSecuencia e = new ProcedimientoPasoSecuencia();
        e.setIdProcedimientoPasoSecuencia(id);
        when(procedimientoPasoSecuenciaRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoPasoSecuenciaService.actualizar(e));
        verify(procedimientoPasoSecuenciaRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        ProcedimientoPasoSecuencia e = new ProcedimientoPasoSecuencia();
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoSecuenciaRepository).create(any());

        assertThrows(ServiceException.class, () -> procedimientoPasoSecuenciaService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ProcedimientoPasoSecuencia e = new ProcedimientoPasoSecuencia();
        e.setIdProcedimientoPasoSecuencia(id);
        when(procedimientoPasoSecuenciaRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoSecuenciaRepository).update(e);

        assertThrows(ServiceException.class, () -> procedimientoPasoSecuenciaService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ProcedimientoPasoSecuencia e = new ProcedimientoPasoSecuencia();
        when(procedimientoPasoSecuenciaRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoSecuenciaRepository).delete(id);

        assertThrows(ServiceException.class, () -> procedimientoPasoSecuenciaService.eliminar(id));
    }
}
