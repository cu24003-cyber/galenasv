package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimiento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ConsultaProcedimientoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaProcedimientoServiceTest {

    @Mock
    private ConsultaProcedimientoRepository consultaProcedimientoRepository;

    @InjectMocks
    private ConsultaProcedimientoService consultaProcedimientoService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        ConsultaProcedimiento esperada = new ConsultaProcedimiento();
        
        when(consultaProcedimientoRepository.find(id)).thenReturn(esperada);

        ConsultaProcedimiento resultado = consultaProcedimientoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(consultaProcedimientoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(consultaProcedimientoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> consultaProcedimientoService.eliminar(id));

        verify(consultaProcedimientoRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        ConsultaProcedimiento e = new ConsultaProcedimiento();
        consultaProcedimientoService.crear(e);
        assertNotNull(e.getIdConsultaProcedimiento());
        verify(consultaProcedimientoRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        ConsultaProcedimiento e = new ConsultaProcedimiento();
        e.setIdConsultaProcedimiento(idFijo);
        consultaProcedimientoService.crear(e);
        assertEquals(idFijo, e.getIdConsultaProcedimiento());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        ConsultaProcedimiento e = new ConsultaProcedimiento();
        e.setIdConsultaProcedimiento(id);
        when(consultaProcedimientoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> consultaProcedimientoService.actualizar(e));
        verify(consultaProcedimientoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        ConsultaProcedimiento e = new ConsultaProcedimiento();
        doThrow(new PersistenceException("Error BD")).when(consultaProcedimientoRepository).create(any());

        assertThrows(ServiceException.class, () -> consultaProcedimientoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ConsultaProcedimiento e = new ConsultaProcedimiento();
        e.setIdConsultaProcedimiento(id);
        when(consultaProcedimientoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(consultaProcedimientoRepository).update(e);

        assertThrows(ServiceException.class, () -> consultaProcedimientoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ConsultaProcedimiento e = new ConsultaProcedimiento();
        when(consultaProcedimientoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(consultaProcedimientoRepository).delete(id);

        assertThrows(ServiceException.class, () -> consultaProcedimientoService.eliminar(id));
    }
}
