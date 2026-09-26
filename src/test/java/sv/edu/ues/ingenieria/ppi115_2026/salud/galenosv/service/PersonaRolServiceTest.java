package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.PersonaRolRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaRolServiceTest {

    @Mock
    private PersonaRolRepository personaRolRepository;

    @InjectMocks
    private PersonaRolService personaRolService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        PersonaRol esperada = new PersonaRol();
        
        when(personaRolRepository.find(id)).thenReturn(esperada);

        PersonaRol resultado = personaRolService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(personaRolRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(personaRolRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> personaRolService.eliminar(id));

        verify(personaRolRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        PersonaRol e = new PersonaRol();
        personaRolService.crear(e);
        assertNotNull(e.getIdPersonaRol());
        verify(personaRolRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        PersonaRol e = new PersonaRol();
        e.setIdPersonaRol(idFijo);
        personaRolService.crear(e);
        assertEquals(idFijo, e.getIdPersonaRol());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        PersonaRol e = new PersonaRol();
        e.setIdPersonaRol(id);
        when(personaRolRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> personaRolService.actualizar(e));
        verify(personaRolRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        PersonaRol e = new PersonaRol();
        doThrow(new PersistenceException("Error BD")).when(personaRolRepository).create(any());

        assertThrows(ServiceException.class, () -> personaRolService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        PersonaRol e = new PersonaRol();
        e.setIdPersonaRol(id);
        when(personaRolRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(personaRolRepository).update(e);

        assertThrows(ServiceException.class, () -> personaRolService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        PersonaRol e = new PersonaRol();
        when(personaRolRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(personaRolRepository).delete(id);

        assertThrows(ServiceException.class, () -> personaRolService.eliminar(id));
    }
}
