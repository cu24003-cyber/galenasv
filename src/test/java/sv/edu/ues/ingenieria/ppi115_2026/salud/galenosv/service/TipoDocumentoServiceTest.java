package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.TipoDocumentoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoDocumentoServiceTest {

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @InjectMocks
    private TipoDocumentoService tipoDocumentoService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        TipoDocumento esperada = new TipoDocumento();
        
        when(tipoDocumentoRepository.find(id)).thenReturn(esperada);

        TipoDocumento resultado = tipoDocumentoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(tipoDocumentoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(tipoDocumentoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> tipoDocumentoService.eliminar(id));

        verify(tipoDocumentoRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        TipoDocumento e = new TipoDocumento();
        tipoDocumentoService.crear(e);
        assertNotNull(e.getIdTipoDocumento());
        verify(tipoDocumentoRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        TipoDocumento e = new TipoDocumento();
        e.setIdTipoDocumento(idFijo);
        tipoDocumentoService.crear(e);
        assertEquals(idFijo, e.getIdTipoDocumento());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        TipoDocumento e = new TipoDocumento();
        e.setIdTipoDocumento(id);
        when(tipoDocumentoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> tipoDocumentoService.actualizar(e));
        verify(tipoDocumentoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        TipoDocumento e = new TipoDocumento();
        doThrow(new PersistenceException("Error BD")).when(tipoDocumentoRepository).create(any());

        assertThrows(ServiceException.class, () -> tipoDocumentoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        TipoDocumento e = new TipoDocumento();
        e.setIdTipoDocumento(id);
        when(tipoDocumentoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(tipoDocumentoRepository).update(e);

        assertThrows(ServiceException.class, () -> tipoDocumentoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        TipoDocumento e = new TipoDocumento();
        when(tipoDocumentoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(tipoDocumentoRepository).delete(id);

        assertThrows(ServiceException.class, () -> tipoDocumentoService.eliminar(id));
    }
}
