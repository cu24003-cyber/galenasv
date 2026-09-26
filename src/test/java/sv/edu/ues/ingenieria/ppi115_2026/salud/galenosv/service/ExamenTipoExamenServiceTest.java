package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenTipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ExamenTipoExamenRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamenTipoExamenServiceTest {

    @Mock
    private ExamenTipoExamenRepository examenTipoExamenRepository;

    @InjectMocks
    private ExamenTipoExamenService examenTipoExamenService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        ExamenTipoExamen esperada = new ExamenTipoExamen();
        
        when(examenTipoExamenRepository.find(id)).thenReturn(esperada);

        ExamenTipoExamen resultado = examenTipoExamenService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(examenTipoExamenRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(examenTipoExamenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> examenTipoExamenService.eliminar(id));

        verify(examenTipoExamenRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        ExamenTipoExamen e = new ExamenTipoExamen();
        examenTipoExamenService.crear(e);
        assertNotNull(e.getIdExamenTipoExamen());
        verify(examenTipoExamenRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        ExamenTipoExamen e = new ExamenTipoExamen();
        e.setIdExamenTipoExamen(idFijo);
        examenTipoExamenService.crear(e);
        assertEquals(idFijo, e.getIdExamenTipoExamen());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        ExamenTipoExamen e = new ExamenTipoExamen();
        e.setIdExamenTipoExamen(id);
        when(examenTipoExamenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> examenTipoExamenService.actualizar(e));
        verify(examenTipoExamenRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        ExamenTipoExamen e = new ExamenTipoExamen();
        doThrow(new PersistenceException("Error BD")).when(examenTipoExamenRepository).create(any());

        assertThrows(ServiceException.class, () -> examenTipoExamenService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ExamenTipoExamen e = new ExamenTipoExamen();
        e.setIdExamenTipoExamen(id);
        when(examenTipoExamenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(examenTipoExamenRepository).update(e);

        assertThrows(ServiceException.class, () -> examenTipoExamenService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ExamenTipoExamen e = new ExamenTipoExamen();
        when(examenTipoExamenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(examenTipoExamenRepository).delete(id);

        assertThrows(ServiceException.class, () -> examenTipoExamenService.eliminar(id));
    }
}
