package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Consulta;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ConsultaRepository;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    @Mock
    private ConsultaRepository consultaRepository;

    @InjectMocks
    private ConsultaService consultaService;

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        Consulta c = new Consulta();
        consultaService.crear(c);
        assertNotNull(c.getIdConsulta());
        verify(consultaRepository).create(c);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        Consulta c = new Consulta();
        c.setIdConsulta(idFijo);
        consultaService.crear(c);
        assertEquals(idFijo, c.getIdConsulta());
    }

    @Test
    void listarPorPersona_delegaAlRepository() {
        UUID idPersona = UUID.randomUUID();
        Consulta c1 = new Consulta();
        List<Consulta> esperado = List.of(c1);
        when(consultaRepository.listarPorPersona(idPersona)).thenReturn(esperado);

        List<Consulta> resultado = consultaService.listarPorPersona(idPersona);

        assertEquals(esperado, resultado);
        verify(consultaRepository).listarPorPersona(idPersona);
    }

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        Consulta esperada = new Consulta();
        when(consultaRepository.find(id)).thenReturn(esperada);

        Consulta resultado = consultaService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(consultaRepository).find(id);
    }

    @Test
    void eliminar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(consultaRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> consultaService.eliminar(id));
        verify(consultaRepository, never()).delete(any());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        Consulta e = new Consulta();
        e.setIdConsulta(id);
        when(consultaRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> consultaService.actualizar(e));
        verify(consultaRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        Consulta e = new Consulta();
        doThrow(new PersistenceException("Error BD")).when(consultaRepository).create(any());

        assertThrows(ServiceException.class, () -> consultaService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Consulta e = new Consulta();
        e.setIdConsulta(id);
        when(consultaRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(consultaRepository).update(e);

        assertThrows(ServiceException.class, () -> consultaService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Consulta e = new Consulta();
        when(consultaRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(consultaRepository).delete(id);

        assertThrows(ServiceException.class, () -> consultaService.eliminar(id));
    }
}