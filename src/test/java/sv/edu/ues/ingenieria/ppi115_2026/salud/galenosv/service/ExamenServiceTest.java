package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ExamenRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamenServiceTest {

    @Mock
    private ExamenRepository examenRepository;

    @InjectMocks
    private ExamenService examenService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        Examen esperada = new Examen();
        
        when(examenRepository.find(id)).thenReturn(esperada);

        Examen resultado = examenService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(examenRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(examenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> examenService.eliminar(id));

        verify(examenRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        Examen e = new Examen();
        examenService.crear(e);
        assertNotNull(e.getIdExamen());
        verify(examenRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        Examen e = new Examen();
        e.setIdExamen(idFijo);
        examenService.crear(e);
        assertEquals(idFijo, e.getIdExamen());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        Examen e = new Examen();
        e.setIdExamen(id);
        when(examenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> examenService.actualizar(e));
        verify(examenRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        Examen e = new Examen();
        doThrow(new PersistenceException("Error BD")).when(examenRepository).create(any());

        assertThrows(ServiceException.class, () -> examenService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Examen e = new Examen();
        e.setIdExamen(id);
        when(examenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(examenRepository).update(e);

        assertThrows(ServiceException.class, () -> examenService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Examen e = new Examen();
        when(examenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(examenRepository).delete(id);

        assertThrows(ServiceException.class, () -> examenService.eliminar(id));
    }
}
