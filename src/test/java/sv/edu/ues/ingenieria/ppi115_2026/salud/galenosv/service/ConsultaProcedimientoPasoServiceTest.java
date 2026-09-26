package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimientoPaso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ConsultaProcedimientoPasoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaProcedimientoPasoServiceTest {

    @Mock
    private ConsultaProcedimientoPasoRepository consultaProcedimientoPasoRepository;

    @InjectMocks
    private ConsultaProcedimientoPasoService consultaProcedimientoPasoService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        ConsultaProcedimientoPaso esperada = new ConsultaProcedimientoPaso();
        
        when(consultaProcedimientoPasoRepository.find(id)).thenReturn(esperada);

        ConsultaProcedimientoPaso resultado = consultaProcedimientoPasoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(consultaProcedimientoPasoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(consultaProcedimientoPasoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> consultaProcedimientoPasoService.eliminar(id));

        verify(consultaProcedimientoPasoRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        ConsultaProcedimientoPaso e = new ConsultaProcedimientoPaso();
        consultaProcedimientoPasoService.crear(e);
        assertNotNull(e.getIdConsultaProcedimientoPaso());
        verify(consultaProcedimientoPasoRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        ConsultaProcedimientoPaso e = new ConsultaProcedimientoPaso();
        e.setIdConsultaProcedimientoPaso(idFijo);
        consultaProcedimientoPasoService.crear(e);
        assertEquals(idFijo, e.getIdConsultaProcedimientoPaso());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        ConsultaProcedimientoPaso e = new ConsultaProcedimientoPaso();
        e.setIdConsultaProcedimientoPaso(id);
        when(consultaProcedimientoPasoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> consultaProcedimientoPasoService.actualizar(e));
        verify(consultaProcedimientoPasoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        ConsultaProcedimientoPaso e = new ConsultaProcedimientoPaso();
        doThrow(new PersistenceException("Error BD")).when(consultaProcedimientoPasoRepository).create(any());

        assertThrows(ServiceException.class, () -> consultaProcedimientoPasoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ConsultaProcedimientoPaso e = new ConsultaProcedimientoPaso();
        e.setIdConsultaProcedimientoPaso(id);
        when(consultaProcedimientoPasoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(consultaProcedimientoPasoRepository).update(e);

        assertThrows(ServiceException.class, () -> consultaProcedimientoPasoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ConsultaProcedimientoPaso e = new ConsultaProcedimientoPaso();
        when(consultaProcedimientoPasoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(consultaProcedimientoPasoRepository).delete(id);

        assertThrows(ServiceException.class, () -> consultaProcedimientoPasoService.eliminar(id));
    }
}
