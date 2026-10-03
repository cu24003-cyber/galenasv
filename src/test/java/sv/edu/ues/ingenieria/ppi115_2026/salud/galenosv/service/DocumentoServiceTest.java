package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.DocumentoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.TipoDocumentoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @InjectMocks
    private DocumentoService documentoService;

    private Documento dui() {
        Persona persona = new Persona(UUID.randomUUID());
        TipoDocumento tipo = new TipoDocumento(UUID.randomUUID()); tipo.setNombre(" DUI ");
        Documento documento = new Documento(); documento.setIdPersona(persona); documento.setIdTipoDocumento(tipo);
        when(tipoDocumentoRepository.find(tipo.getIdTipoDocumento())).thenReturn(tipo);
        when(documentoRepository.bloquearPersona(persona.getIdPersona())).thenReturn(persona);
        return documento;
    }

    @Test
    void rechazaSegundoDuiAunqueTengaNumeroDiferente() {
        Documento documento = dui(); documento.setValor("87654321-0");
        when(documentoRepository.existeDuiPorPersona(eq(documento.getIdPersona().getIdPersona()), any())).thenReturn(true);
        ServiceException error = assertThrows(ServiceException.class, () -> documentoService.crear(documento));
        assertEquals("documento.duiUnico", error.getMessageKey());
        verify(documentoRepository, never()).create(any());
    }

    @Test
    void permitePrimerDuiYBloqueaPersonaAntesDeComprobarDuplicados() {
        Documento documento = dui();
        documentoService.crear(documento);
        var orden = inOrder(documentoRepository);
        orden.verify(documentoRepository).bloquearPersona(documento.getIdPersona().getIdPersona());
        orden.verify(documentoRepository).existeDuiPorPersona(documento.getIdPersona().getIdPersona(), documento.getIdDocumento());
        orden.verify(documentoRepository).create(documento);
    }

    @Test
    void permiteActualizarElMismoDuiExcluyendoSuId() {
        Documento documento = dui(); documento.setIdDocumento(UUID.randomUUID());
        when(documentoRepository.find(documento.getIdDocumento())).thenReturn(documento);
        documentoService.actualizar(documento);
        verify(documentoRepository).existeDuiPorPersona(documento.getIdPersona().getIdPersona(), documento.getIdDocumento());
        verify(documentoRepository).update(documento);
    }

    @Test
    void impideConvertirOtroDocumentoEnSegundoDui() {
        Documento documento = dui(); documento.setIdDocumento(UUID.randomUUID());
        when(documentoRepository.existeDuiPorPersona(documento.getIdPersona().getIdPersona(), documento.getIdDocumento())).thenReturn(true);
        assertThrows(ServiceException.class, () -> documentoService.actualizar(documento));
        verify(documentoRepository, never()).update(any());
    }

    @Test
    void validaConNombrePersistidoYReconoceNombreCompletoDelDui() {
        Documento documento = dui();
        TipoDocumento persistido = new TipoDocumento(documento.getIdTipoDocumento().getIdTipoDocumento());
        persistido.setNombre("Documento Único de Identidad");
        documento.getIdTipoDocumento().setNombre("Pasaporte");
        when(tipoDocumentoRepository.find(persistido.getIdTipoDocumento())).thenReturn(persistido);
        when(documentoRepository.existeDuiPorPersona(eq(documento.getIdPersona().getIdPersona()), any())).thenReturn(true);
        assertThrows(ServiceException.class, () -> documentoService.crear(documento));
        verify(documentoRepository, never()).create(any());
    }

    @Test
    void permiteVariosDocumentosDeOtrosTipos() {
        TipoDocumento tipo = new TipoDocumento(UUID.randomUUID()); tipo.setNombre("Pasaporte");
        when(tipoDocumentoRepository.find(tipo.getIdTipoDocumento())).thenReturn(tipo);
        for (int i = 0; i < 2; i++) {
            Documento documento = new Documento(); documento.setIdTipoDocumento(tipo);
            documento.setIdPersona(new Persona(UUID.randomUUID())); documentoService.crear(documento);
        }
        verify(documentoRepository, times(2)).create(any());
        verify(documentoRepository, never()).existeDuiPorPersona(any(), any());
    }

    @Test
    void eliminaSoloDocumentoDeLaPersonaSeleccionada() {
        Documento documento = new Documento(UUID.randomUUID()); documento.setIdPersona(new Persona(UUID.randomUUID()));
        when(documentoRepository.find(documento.getIdDocumento())).thenReturn(documento);
        assertThrows(ServiceException.class, () -> documentoService.eliminarDePersona(documento.getIdDocumento(), UUID.randomUUID()));
        verify(documentoRepository, never()).delete(any());
        documentoService.eliminarDePersona(documento.getIdDocumento(), documento.getIdPersona().getIdPersona());
        verify(documentoRepository).delete(documento.getIdDocumento());
    }

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
