package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.MedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.MedioContactoRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedioContactoServiceTest {

    @Mock
    private MedioContactoRepository medioContactoRepository;

    @InjectMocks
    private MedioContactoService medioContactoService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        MedioContacto esperada = new MedioContacto();
        
        when(medioContactoRepository.find(id)).thenReturn(esperada);

        MedioContacto resultado = medioContactoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(medioContactoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(medioContactoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> medioContactoService.eliminar(id));

        verify(medioContactoRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        MedioContacto e = new MedioContacto();
        medioContactoService.crear(e);
        assertNotNull(e.getIdMedioContacto());
        verify(medioContactoRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        MedioContacto e = new MedioContacto();
        e.setIdMedioContacto(idFijo);
        medioContactoService.crear(e);
        assertEquals(idFijo, e.getIdMedioContacto());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        MedioContacto e = new MedioContacto();
        e.setIdMedioContacto(id);
        when(medioContactoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> medioContactoService.actualizar(e));
        verify(medioContactoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        MedioContacto e = new MedioContacto();
        doThrow(new PersistenceException("Error BD")).when(medioContactoRepository).create(any());

        assertThrows(ServiceException.class, () -> medioContactoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        MedioContacto e = new MedioContacto();
        e.setIdMedioContacto(id);
        when(medioContactoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(medioContactoRepository).update(e);

        assertThrows(ServiceException.class, () -> medioContactoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        MedioContacto e = new MedioContacto();
        when(medioContactoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(medioContactoRepository).delete(id);

        assertThrows(ServiceException.class, () -> medioContactoService.eliminar(id));
    }
}
