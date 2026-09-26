package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Procedimiento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ProcedimientoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcedimientoServiceTest {

    @Mock
    private ProcedimientoRepository procedimientoRepository;

    @InjectMocks
    private ProcedimientoService procedimientoService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        Procedimiento esperada = new Procedimiento();
        
        when(procedimientoRepository.find(id)).thenReturn(esperada);

        Procedimiento resultado = procedimientoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(procedimientoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(procedimientoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoService.eliminar(id));

        verify(procedimientoRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        Procedimiento e = new Procedimiento();
        procedimientoService.crear(e);
        assertNotNull(e.getIdProcedimiento());
        verify(procedimientoRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        Procedimiento e = new Procedimiento();
        e.setIdProcedimiento(idFijo);
        procedimientoService.crear(e);
        assertEquals(idFijo, e.getIdProcedimiento());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        Procedimiento e = new Procedimiento();
        e.setIdProcedimiento(id);
        when(procedimientoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoService.actualizar(e));
        verify(procedimientoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        Procedimiento e = new Procedimiento();
        doThrow(new PersistenceException("Error BD")).when(procedimientoRepository).create(any());

        assertThrows(ServiceException.class, () -> procedimientoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Procedimiento e = new Procedimiento();
        e.setIdProcedimiento(id);
        when(procedimientoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoRepository).update(e);

        assertThrows(ServiceException.class, () -> procedimientoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Procedimiento e = new Procedimiento();
        when(procedimientoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoRepository).delete(id);

        assertThrows(ServiceException.class, () -> procedimientoService.eliminar(id));
    }
}
