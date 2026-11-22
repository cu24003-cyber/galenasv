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
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RolRepository;

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

    @Test void noPermiteCambiarLaClasificacionDeUnRolAlEditar() {
        for (String[] nombres : new String[][]{
                {"Paciente", "Doctor"}, {"Doctor", "Paciente"},
                {"Atención al público", "Doctor"}, {"Doctor", "Administración"},
                {"Paciente", "Administración"}, {null, "Paciente"}}) {
            Rol anterior = new Rol(UUID.randomUUID()); anterior.setNombre(nombres[0]);
            Rol cambio = new Rol(anterior.getIdRol()); cambio.setNombre(nombres[1]);
            when(rolRepository.find(anterior.getIdRol())).thenReturn(anterior);
            assertEquals("rol.nombreReservado", assertThrows(ServiceException.class,
                    () -> rolService.actualizar(cambio)).getMessageKey());
        }
        verify(rolRepository, never()).update(any());
    }

    @Test void conservaLaClasificacionAlEditarNombresPermitidos() {
        for (String[] nombres : new String[][]{
                {"Paciente", " PACIENTE "}, {"Atención al público", "Administración"},
                {"Doctor", "Médico general"}}) {
            Rol anterior = new Rol(UUID.randomUUID()); anterior.setNombre(nombres[0]);
            Rol cambio = new Rol(anterior.getIdRol()); cambio.setNombre(nombres[1]);
            when(rolRepository.find(anterior.getIdRol())).thenReturn(anterior);
            rolService.actualizar(cambio);
            verify(rolRepository).update(cambio);
        }
    }

    @Test void validarCambioNombreNormalizaVariantesDeLosRolesReservados() {
        for (String[] nombres : new String[][]{
                {"Atención al público", "ATENCION  AL PUBLICO"}, {"Administración", "Atencion al publico"},
                {"Paciente", "  paciénte "}}) {
            Rol anterior = new Rol(UUID.randomUUID()); anterior.setNombre(nombres[0]);
            assertDoesNotThrow(() -> RolService.validarCambioNombre(anterior, nombres[1]));
        }
        Rol admin = new Rol(UUID.randomUUID()); admin.setNombre("ATENCIÓN  AL PÚBLICO");
        assertEquals("rol.nombreReservado", assertThrows(ServiceException.class,
                () -> RolService.validarCambioNombre(admin, "Doctor")).getMessageKey());
        Rol doctor = new Rol(UUID.randomUUID()); doctor.setNombre("Doctor");
        assertThrows(ServiceException.class, () -> RolService.validarCambioNombre(doctor, "Administracion"));
    }

    private Rol rol(String nombre) {
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre(nombre); return rol;
    }

    @Test void crearRechazaNombreYaExistenteConLaMismaNormalizacion() {
        when(rolRepository.findAll()).thenReturn(java.util.List.of(rol("Atención al público"), rol("Paciente")));
        for (String nombre : new String[]{"ATENCION  AL PUBLICO", " paciente ", "Pacienté"}) {
            assertEquals("rol.nombreDuplicado", assertThrows(ServiceException.class,
                    () -> rolService.crear(rol(nombre))).getMessageKey(), nombre);
        }
        verify(rolRepository, never()).create(any());
    }

    @Test void crearPermiteNombreNuevo() {
        when(rolRepository.findAll()).thenReturn(java.util.List.of(rol("Paciente")));
        Rol nuevo = rol("Laboratorista");
        rolService.crear(nuevo);
        verify(rolRepository).create(nuevo);
    }

    @Test void actualizarRechazaNombreDeOtroRol() {
        Rol enfermera = rol("Enfermera");
        Rol cambio = rol("  DOCTOR "); cambio.setIdRol(enfermera.getIdRol());
        when(rolRepository.find(enfermera.getIdRol())).thenReturn(enfermera);
        when(rolRepository.findAll()).thenReturn(java.util.List.of(enfermera, rol("Doctor")));
        assertEquals("rol.nombreDuplicado", assertThrows(ServiceException.class,
                () -> rolService.actualizar(cambio)).getMessageKey());
        verify(rolRepository, never()).update(any());
    }

    @Test void actualizarPermiteConservarSuPropioNombre() {
        Rol doctor = rol("Doctor");
        Rol cambio = rol("DOCTOR"); cambio.setIdRol(doctor.getIdRol());
        when(rolRepository.find(doctor.getIdRol())).thenReturn(doctor);
        when(rolRepository.findAll()).thenReturn(java.util.List.of(doctor));
        rolService.actualizar(cambio);
        verify(rolRepository).update(cambio);
    }
}
