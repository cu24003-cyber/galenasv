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
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPaso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ProcedimientoPasoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RolRepository;

@ExtendWith(MockitoExtension.class)
class ProcedimientoPasoServiceTest {

    @Mock
    private ProcedimientoPasoRepository procedimientoPasoRepository;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private ProcedimientoPasoService procedimientoPasoService;

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        ProcedimientoPaso esperada = new ProcedimientoPaso();
        
        when(procedimientoPasoRepository.find(id)).thenReturn(esperada);

        ProcedimientoPaso resultado = procedimientoPasoService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(procedimientoPasoRepository).find(id);
    }

    @Test
    void eliminar_noEjecutaDelete_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(procedimientoPasoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoPasoService.eliminar(id));

        verify(procedimientoPasoRepository, never()).delete(any());
    }

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        ProcedimientoPaso e = new ProcedimientoPaso();
        procedimientoPasoService.crear(e);
        assertNotNull(e.getIdProcedimientoPaso());
        verify(procedimientoPasoRepository).create(e);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        ProcedimientoPaso e = new ProcedimientoPaso();
        e.setIdProcedimientoPaso(idFijo);
        procedimientoPasoService.crear(e);
        assertEquals(idFijo, e.getIdProcedimientoPaso());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        ProcedimientoPaso e = new ProcedimientoPaso();
        e.setIdProcedimientoPaso(id);
        when(procedimientoPasoRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> procedimientoPasoService.actualizar(e));
        verify(procedimientoPasoRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        ProcedimientoPaso e = new ProcedimientoPaso();
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoRepository).create(any());

        assertThrows(ServiceException.class, () -> procedimientoPasoService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ProcedimientoPaso e = new ProcedimientoPaso();
        e.setIdProcedimientoPaso(id);
        when(procedimientoPasoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoRepository).update(e);

        assertThrows(ServiceException.class, () -> procedimientoPasoService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        ProcedimientoPaso e = new ProcedimientoPaso();
        when(procedimientoPasoRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(procedimientoPasoRepository).delete(id);

        assertThrows(ServiceException.class, () -> procedimientoPasoService.eliminar(id));
    }

    private Rol rol(boolean activo) {
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre("Farmaceutico"); rol.setActivo(activo);
        lenient().when(rolRepository.find(rol.getIdRol())).thenReturn(rol);
        return rol;
    }

    @Test
    void crearRechazaUnPasoConRolInactivo() {
        ProcedimientoPaso paso = new ProcedimientoPaso();
        paso.setIdRol(new Rol(rol(false).getIdRol()));
        assertEquals("procedimiento.pasoNombreRol", assertThrows(ServiceException.class,
                () -> procedimientoPasoService.crear(paso)).getMessageKey());
        verify(procedimientoPasoRepository, never()).create(any());
    }

    @Test
    void crearRechazaUnRolInexistente() {
        ProcedimientoPaso paso = new ProcedimientoPaso();
        paso.setIdRol(new Rol(UUID.randomUUID()));
        assertThrows(ServiceException.class, () -> procedimientoPasoService.crear(paso));
        verify(procedimientoPasoRepository, never()).create(any());
    }

    @Test
    void crearAceptaUnRolActivo() {
        ProcedimientoPaso paso = new ProcedimientoPaso();
        paso.setIdRol(new Rol(rol(true).getIdRol()));
        procedimientoPasoService.crear(paso);
        verify(procedimientoPasoRepository).create(paso);
    }

    @Test
    void actualizarRechazaCambiarAUnRolInactivoPeroConservaElQueYaTenia() {
        Rol inactivo = rol(false);
        ProcedimientoPaso guardado = new ProcedimientoPaso(UUID.randomUUID()); guardado.setIdRol(inactivo);
        when(procedimientoPasoRepository.find(guardado.getIdProcedimientoPaso())).thenReturn(guardado);
        ProcedimientoPaso renombrado = new ProcedimientoPaso(guardado.getIdProcedimientoPaso());
        renombrado.setIdRol(new Rol(inactivo.getIdRol())); renombrado.setNombre("Otro nombre");
        procedimientoPasoService.actualizar(renombrado);
        verify(procedimientoPasoRepository).update(renombrado);

        ProcedimientoPaso otro = new ProcedimientoPaso(UUID.randomUUID()); otro.setIdRol(rol(true));
        when(procedimientoPasoRepository.find(otro.getIdProcedimientoPaso())).thenReturn(otro);
        ProcedimientoPaso cambio = new ProcedimientoPaso(otro.getIdProcedimientoPaso()); cambio.setIdRol(new Rol(inactivo.getIdRol()));
        assertThrows(ServiceException.class, () -> procedimientoPasoService.actualizar(cambio));
        verify(procedimientoPasoRepository, never()).update(cambio);
    }
}
