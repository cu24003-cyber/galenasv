package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.TipoExamenRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoExamenServiceTest {

    @Mock
    private TipoExamenRepository tipoExamenRepository;

    @InjectMocks
    private TipoExamenService tipoExamenService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        TipoExamen esperada = new TipoExamen();
        
        when(tipoExamenRepository.find(id)).thenReturn(esperada);

        TipoExamen resultado = tipoExamenService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(tipoExamenRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(tipoExamenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> tipoExamenService.eliminar(id));

        verify(tipoExamenRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        TipoExamen e = new TipoExamen();
        tipoExamenService.crear(e);
        assertNotNull(e.getIdTipoExamen());
        verify(tipoExamenRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        TipoExamen e = new TipoExamen();
        e.setIdTipoExamen(idFijo);
        tipoExamenService.crear(e);
        assertEquals(idFijo, e.getIdTipoExamen());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        TipoExamen e = new TipoExamen();
        e.setIdTipoExamen(id);
        when(tipoExamenRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> tipoExamenService.actualizar(e));
        verify(tipoExamenRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        TipoExamen e = new TipoExamen();
        doThrow(new PersistenceException("Error BD")).when(tipoExamenRepository).create(any());

        assertThrows(ServiceException.class, () -> tipoExamenService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        TipoExamen e = new TipoExamen();
        e.setIdTipoExamen(id);
        when(tipoExamenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(tipoExamenRepository).update(e);

        assertThrows(ServiceException.class, () -> tipoExamenService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        TipoExamen e = new TipoExamen();
        when(tipoExamenRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(tipoExamenRepository).delete(id);

        assertThrows(ServiceException.class, () -> tipoExamenService.eliminar(id));
    }
}
