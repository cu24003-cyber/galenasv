package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.DocumentoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @InjectMocks
    private DocumentoService documentoService;

    @Test
    void crear_asignaUUID_siIdEsNulo() throws Exception {
        Documento documento = new Documento();
        documento.setIdDocumento(null);

        documentoService.crear(documento);

        assertNotNull(documento.getIdDocumento(), "Debe asignar un UUID cuando el ID es nulo");
        verify(documentoRepository).create(documento);
    }

    @Test
    void crear_mantieneId_siIdYaExiste() throws Exception {
        UUID idExistente = UUID.randomUUID();
        Documento documento = new Documento();
        documento.setIdDocumento(idExistente);

        documentoService.crear(documento);

        assertEquals(idExistente, documento.getIdDocumento(), "No debe sobrescribir el UUID si ya tiene uno asignado");
        verify(documentoRepository).create(documento);
    }

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        Documento esperada = new Documento();

        when(documentoRepository.find(id)).thenReturn(esperada);

        Documento resultado = documentoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(documentoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(documentoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> documentoService.eliminar(id));

        verify(documentoRepository, never()).delete(any());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        Documento e = new Documento();
        e.setIdDocumento(id);
        when(documentoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> documentoService.actualizar(e));
        verify(documentoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        Documento e = new Documento();
        doThrow(new PersistenceException("Error BD")).when(documentoRepository).create(any());

        assertThrows(ServiceException.class, () -> documentoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Documento e = new Documento();
        e.setIdDocumento(id);
        when(documentoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(documentoRepository).update(e);

        assertThrows(ServiceException.class, () -> documentoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Documento e = new Documento();
        when(documentoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(documentoRepository).delete(id);

        assertThrows(ServiceException.class, () -> documentoService.eliminar(id));
    }
}