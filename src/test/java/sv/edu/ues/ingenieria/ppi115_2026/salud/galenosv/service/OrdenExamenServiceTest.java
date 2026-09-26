package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.OrdenExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.OrdenExamenRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenExamenServiceTest {

    @Mock
    private OrdenExamenRepository ordenExamenRepository;

    @InjectMocks
    private OrdenExamenService ordenExamenService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        OrdenExamen esperada = new OrdenExamen();
        
        when(ordenExamenRepository.find(id)).thenReturn(esperada);

        OrdenExamen resultado = ordenExamenService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(ordenExamenRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(ordenExamenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> ordenExamenService.eliminar(id));

        verify(ordenExamenRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        OrdenExamen e = new OrdenExamen();
        ordenExamenService.crear(e);
        assertNotNull(e.getIdOrdenExamen());
        verify(ordenExamenRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        OrdenExamen e = new OrdenExamen();
        e.setIdOrdenExamen(idFijo);
        ordenExamenService.crear(e);
        assertEquals(idFijo, e.getIdOrdenExamen());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        OrdenExamen e = new OrdenExamen();
        e.setIdOrdenExamen(id);
        when(ordenExamenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> ordenExamenService.actualizar(e));
        verify(ordenExamenRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        OrdenExamen e = new OrdenExamen();
        doThrow(new PersistenceException("Error BD")).when(ordenExamenRepository).create(any());

        assertThrows(ServiceException.class, () -> ordenExamenService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        OrdenExamen e = new OrdenExamen();
        e.setIdOrdenExamen(id);
        when(ordenExamenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(ordenExamenRepository).update(e);

        assertThrows(ServiceException.class, () -> ordenExamenService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        OrdenExamen e = new OrdenExamen();
        when(ordenExamenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(ordenExamenRepository).delete(id);

        assertThrows(ServiceException.class, () -> ordenExamenService.eliminar(id));
    }
}
