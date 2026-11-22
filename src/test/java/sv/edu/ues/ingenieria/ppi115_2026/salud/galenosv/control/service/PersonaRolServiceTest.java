package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.persistence.PersistenceException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.PersonaRolRepository;

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

    private PersonaRol asignacion(UUID id, boolean conRol) {
        PersonaRol e = new PersonaRol(); e.setIdPersonaRol(id);
        e.setIdPersona(new sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona(UUID.randomUUID()));
        if (conRol) e.setIdRol(new sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol(UUID.randomUUID()));
        e.setIdClinica(new sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Clinica(UUID.randomUUID()));
        return e;
    }

    @Test
    void crearRechazaPersonaRolYClinicaDuplicados() {
        PersonaRol e = asignacion(null, true);
        when(personaRolRepository.existeAsignacion(e.getIdPersona().getIdPersona(), e.getIdRol().getIdRol(),
                e.getIdClinica().getIdClinica(), null)).thenReturn(true);
        assertEquals("personaRol.duplicada", assertThrows(ServiceException.class,
                () -> personaRolService.crear(e)).getMessageKey());
        verify(personaRolRepository, never()).create(any());
    }

    @Test
    void actualizarRechazaConvertirseEnOtraAsignacionExistente() {
        PersonaRol e = asignacion(UUID.randomUUID(), false);
        when(personaRolRepository.existeAsignacion(e.getIdPersona().getIdPersona(), null,
                e.getIdClinica().getIdClinica(), e.getIdPersonaRol())).thenReturn(true);
        assertThrows(ServiceException.class, () -> personaRolService.actualizar(e));
        verify(personaRolRepository, never()).update(any());
    }

    @Test
    void crearPermiteAsignacionNueva() {
        PersonaRol e = asignacion(null, true);
        personaRolService.crear(e);
        verify(personaRolRepository).existeAsignacion(e.getIdPersona().getIdPersona(), e.getIdRol().getIdRol(),
                e.getIdClinica().getIdClinica(), null);
        verify(personaRolRepository).create(e);
    }
}
