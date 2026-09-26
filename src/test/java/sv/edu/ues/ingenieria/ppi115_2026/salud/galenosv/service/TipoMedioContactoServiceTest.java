package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.TipoMedioContactoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoMedioContactoServiceTest {

    @Mock
    private TipoMedioContactoRepository tipoMedioContactoRepository;

    @InjectMocks
    private TipoMedioContactoService tipoMedioContactoService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        TipoMedioContacto esperada = new TipoMedioContacto();
        
        when(tipoMedioContactoRepository.find(id)).thenReturn(esperada);

        TipoMedioContacto resultado = tipoMedioContactoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(tipoMedioContactoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(tipoMedioContactoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> tipoMedioContactoService.eliminar(id));

        verify(tipoMedioContactoRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        TipoMedioContacto e = new TipoMedioContacto();
        tipoMedioContactoService.crear(e);
        assertNotNull(e.getIdTipoMedioContacto());
        verify(tipoMedioContactoRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        TipoMedioContacto e = new TipoMedioContacto();
        e.setIdTipoMedioContacto(idFijo);
        tipoMedioContactoService.crear(e);
        assertEquals(idFijo, e.getIdTipoMedioContacto());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        TipoMedioContacto e = new TipoMedioContacto();
        e.setIdTipoMedioContacto(id);
        when(tipoMedioContactoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> tipoMedioContactoService.actualizar(e));
        verify(tipoMedioContactoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        TipoMedioContacto e = new TipoMedioContacto();
        doThrow(new PersistenceException("Error BD")).when(tipoMedioContactoRepository).create(any());

        assertThrows(ServiceException.class, () -> tipoMedioContactoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        TipoMedioContacto e = new TipoMedioContacto();
        e.setIdTipoMedioContacto(id);
        when(tipoMedioContactoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(tipoMedioContactoRepository).update(e);

        assertThrows(ServiceException.class, () -> tipoMedioContactoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        TipoMedioContacto e = new TipoMedioContacto();
        when(tipoMedioContactoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(tipoMedioContactoRepository).delete(id);

        assertThrows(ServiceException.class, () -> tipoMedioContactoService.eliminar(id));
    }
}
