package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.PersonaRepository;

import java.util.Date;
import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @InjectMocks
    private PersonaService personaService;

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        Persona p = new Persona();
        personaService.crear(p);
        assertNotNull(p.getIdPersona());
        verify(personaRepository).create(p);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        Persona p = new Persona();
        p.setIdPersona(idFijo);
        personaService.crear(p);
        assertEquals(idFijo, p.getIdPersona());
    }

    @Test
    void crear_asignaFechaCreacion_cuandoEsNula() {
        Persona p = new Persona();
        assertNull(p.getFechaCreacion());
        personaService.crear(p);
        assertNotNull(p.getFechaCreacion());
    }

    @Test
    void crear_noSobreescribeFechaCreacion_siYaTieneUna() {
        Date fechaFija = new Date(0L);
        Persona p = new Persona();
        p.setFechaCreacion(fechaFija);
        personaService.crear(p);
        assertEquals(fechaFija, p.getFechaCreacion());
    }

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        Persona esperada = new Persona();
        when(personaRepository.find(id)).thenReturn(esperada);

        Persona resultado = personaService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(personaRepository).find(id);
    }

    @Test
    void eliminar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(personaRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> personaService.eliminar(id));
        verify(personaRepository, never()).delete(any());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        Persona e = new Persona();
        e.setIdPersona(id);
        when(personaRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> personaService.actualizar(e));
        verify(personaRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        Persona e = new Persona();
        doThrow(new PersistenceException("Error BD")).when(personaRepository).create(any());

        assertThrows(ServiceException.class, () -> personaService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Persona e = new Persona();
        e.setIdPersona(id);
        when(personaRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(personaRepository).update(e);

        assertThrows(ServiceException.class, () -> personaService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Persona e = new Persona();
        when(personaRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(personaRepository).delete(id);

        assertThrows(ServiceException.class, () -> personaService.eliminar(id));
    }
}