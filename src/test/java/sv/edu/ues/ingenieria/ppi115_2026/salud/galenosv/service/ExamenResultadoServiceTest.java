package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenResultado;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ExamenResultadoRepository;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamenResultadoServiceTest {

    @Mock
    private ExamenResultadoRepository examenResultadoRepository;

    @InjectMocks
    private ExamenResultadoService examenResultadoService;

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        ExamenResultado er = new ExamenResultado();
        examenResultadoService.crear(er);
        assertNotNull(er.getIdExamenResultado());
        verify(examenResultadoRepository).create(er);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        ExamenResultado er = new ExamenResultado();
        er.setIdExamenResultado(idFijo);
        examenResultadoService.crear(er);
        assertEquals(idFijo, er.getIdExamenResultado());
    }

    @Test
    void listarPorOrdenExamen_delegaAlRepository() {
        UUID idOrdenExamen = UUID.randomUUID();
        ExamenResultado er1 = new ExamenResultado();
        List<ExamenResultado> esperado = List.of(er1);
        when(examenResultadoRepository.listarPorOrdenExamen(idOrdenExamen)).thenReturn(esperado);

        List<ExamenResultado> resultado = examenResultadoService.listarPorOrdenExamen(idOrdenExamen);

        assertEquals(esperado, resultado);
        verify(examenResultadoRepository).listarPorOrdenExamen(idOrdenExamen);
    }

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        ExamenResultado esperada = new ExamenResultado();
        when(examenResultadoRepository.find(id)).thenReturn(esperada);

        ExamenResultado resultado = examenResultadoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(examenResultadoRepository).find(id);
    }

    @Test
    void eliminar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(examenResultadoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> examenResultadoService.eliminar(id));
        verify(examenResultadoRepository, never()).delete(any());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        ExamenResultado e = new ExamenResultado();
        e.setIdExamenResultado(id);
        when(examenResultadoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> examenResultadoService.actualizar(e));
        verify(examenResultadoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        ExamenResultado e = new ExamenResultado();
        doThrow(new PersistenceException("Error BD")).when(examenResultadoRepository).create(any());

        assertThrows(ServiceException.class, () -> examenResultadoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ExamenResultado e = new ExamenResultado();
        e.setIdExamenResultado(id);
        when(examenResultadoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(examenResultadoRepository).update(e);

        assertThrows(ServiceException.class, () -> examenResultadoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ExamenResultado e = new ExamenResultado();
        when(examenResultadoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(examenResultadoRepository).delete(id);

        assertThrows(ServiceException.class, () -> examenResultadoService.eliminar(id));
    }
}