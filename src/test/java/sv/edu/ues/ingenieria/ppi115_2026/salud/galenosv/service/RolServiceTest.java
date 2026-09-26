package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RolRepository;

import java.util.UUID;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private RolService rolService;

    @Test
    void crear_asignaUUID_siIdEsNulo() throws Exception {
        Rol rol = new Rol();
        rol.setIdRol(null);

        rolService.crear(rol);

        assertNotNull(rol.getIdRol(), "Debe asignar un UUID cuando el ID es nulo");
        verify(rolRepository).create(rol);
    }

    @Test
    void crear_mantieneId_siIdYaExiste() throws Exception {
        UUID idExistente = UUID.randomUUID();
        Rol rol = new Rol();
        rol.setIdRol(idExistente);

        rolService.crear(rol);

        assertEquals(idExistente, rol.getIdRol(), "No debe sobrescribir el UUID si ya tiene uno asignado");
        verify(rolRepository).create(rol);
    }

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        Rol esperada = new Rol();

        when(rolRepository.find(id)).thenReturn(esperada);

        Rol resultado = rolService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(rolRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(rolRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> rolService.eliminar(id));

        verify(rolRepository, never()).delete(any());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        Rol e = new Rol();
        e.setIdRol(id);
        when(rolRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> rolService.actualizar(e));
        verify(rolRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        Rol e = new Rol();
        doThrow(new PersistenceException("Error BD")).when(rolRepository).create(any());

        assertThrows(ServiceException.class, () -> rolService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Rol e = new Rol();
        e.setIdRol(id);
        when(rolRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(rolRepository).update(e);

        assertThrows(ServiceException.class, () -> rolService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Rol e = new Rol();
        when(rolRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(rolRepository).delete(id);

        assertThrows(ServiceException.class, () -> rolService.eliminar(id));
    }
}