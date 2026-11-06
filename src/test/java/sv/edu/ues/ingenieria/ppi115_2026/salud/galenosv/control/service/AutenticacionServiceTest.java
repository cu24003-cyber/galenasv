package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock ContrasenaService contrasenas;
    @InjectMocks AutenticacionService servicio;
    private final UUID idUsuario = UUID.randomUUID(), idPersona = UUID.randomUUID();

    private Usuario usuario(boolean activo) {
        Usuario u = new Usuario(); u.setIdUsuario(idUsuario); u.setActivo(activo);
        u.setPasswordHash("hash-almacenado"); u.setPersona(new Persona(idPersona)); return u;
    }
    private PersonaRol asignacion() {
        PersonaRol r = new PersonaRol(UUID.randomUUID()); r.setIdPersona(new Persona(idPersona)); return r;
    }

    @Test void rechazaDesconocidoConElMismoMensajeYVerificaHashDummy() {
        when(contrasenas.hashAusente()).thenReturn("dummy");
        AccesoException error = assertThrows(AccesoException.class, () -> servicio.iniciar("desconocido", "prueba"));
        assertEquals(401, error.getStatus()); assertEquals("Usuario o contraseña incorrectos.", error.getMessage());
        verify(contrasenas).verificar("prueba", "dummy"); verify(usuarios, never()).asignaciones(any());
    }
    @Test void cuentaDeshabilitadaYPasswordIncorrectoNoCreanContexto() {
        when(usuarios.buscarAcceso("julio")).thenReturn(usuario(false));
        when(contrasenas.verificar("prueba", "hash-almacenado")).thenReturn(true);
        assertEquals(401, assertThrows(AccesoException.class, () -> servicio.iniciar("julio", "prueba")).getStatus());
        verify(usuarios, never()).asignaciones(any());
    }
    @Test void iniciaSoloConUnaAsignacionPropiaDevueltaPorElRepositorio() {
        Usuario u = usuario(true); PersonaRol rol = asignacion();
        when(usuarios.buscarAcceso("julio")).thenReturn(u);
        when(contrasenas.verificar(" prueba ", "hash-almacenado")).thenReturn(true);
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(rol));
        var contexto = servicio.iniciar("julio", " prueba ");
        assertSame(u, contexto.usuario()); assertSame(rol, contexto.asignacion());
    }
    @Test void cuentaSinRolOClinicaActivosNoPuedeIniciar() {
        when(usuarios.buscarAcceso("julio")).thenReturn(usuario(true));
        when(contrasenas.verificar(any(), any())).thenReturn(true);
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of());
        assertEquals(403, assertThrows(AccesoException.class, () -> servicio.iniciar("julio", "prueba")).getStatus());
    }
    @Test void noAdmiteUnaAsignacionAjenaAunqueElClienteConozcaSuUuid() {
        when(usuarios.buscarSesion(idUsuario)).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(asignacion()));
        assertEquals(403, assertThrows(AccesoException.class, () -> servicio.contexto(idUsuario, UUID.randomUUID())).getStatus());
    }
    @Test void revalidaLaCuentaEnCadaConsultaDeSesion() {
        when(usuarios.buscarSesion(idUsuario)).thenReturn(usuario(false));
        assertEquals(401, assertThrows(AccesoException.class, () -> servicio.contexto(idUsuario, UUID.randomUUID())).getStatus());
        verify(usuarios, never()).asignaciones(any());
    }
    @Test void recuperaElRolPropioQueSigueActivo() {
        PersonaRol rol = asignacion(); when(usuarios.buscarSesion(idUsuario)).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(rol));
        assertSame(rol, servicio.contexto(idUsuario, rol.getIdPersonaRol()).asignacion());
    }
}
