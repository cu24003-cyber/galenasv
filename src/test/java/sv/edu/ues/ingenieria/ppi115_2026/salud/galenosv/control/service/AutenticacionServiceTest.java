package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {
    @Mock UsuarioRepository usuarios;
    @InjectMocks AutenticacionService servicio;
    private final UUID idUsuario = UUID.randomUUID(), idPersona = UUID.randomUUID();
    private final UUID dui = UUID.randomUUID();
    private static final String MENSAJE = "Documento no registrado o sin acceso autorizado.";

    private Usuario usuario(boolean activo) {
        Usuario u = new Usuario(); u.setIdUsuario(idUsuario); u.setActivo(activo);
        u.setPasswordHash("hash-almacenado"); u.setPersona(new Persona(idPersona)); return u;
    }
    private PersonaRol asignacion() { return asignacion("DOCTOR", 0); }
    private PersonaRol asignacion(String codigo, int dias) {
        PersonaRol r = new PersonaRol(UUID.randomUUID()); r.setIdPersona(new Persona(idPersona));
        Rol rol = new Rol(UUID.randomUUID()); rol.setCodigo(codigo); rol.setActivo(true); r.setIdRol(rol);
        r.setFechaCreacion(Date.from(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(86400L * dias))); return r;
    }
    private void credencialesInvalidas(Runnable accion) {
        AccesoException error = assertThrows(AccesoException.class, accion::run);
        assertEquals(401, error.getStatus()); assertEquals("auth.credenciales", error.getMessageKey());
        assertEquals(MENSAJE, error.getMessage());
    }

    // --- Login por documento (HU2) ---
    @Test void documentoInexistenteSeRechazaSinConsultarAsignaciones() {
        credencialesInvalidas(() -> servicio.iniciar(dui, "00000000-9"));
        verify(usuarios).buscarPorDocumento(dui, "00000000-9"); verify(usuarios, never()).asignaciones(any());
    }
    @Test void documentoDeOtroTipoSeBuscaConElTipoRecibidoYSeRechaza() {
        UUID pasaporte = UUID.randomUUID();
        lenient().when(usuarios.buscarPorDocumento(dui, "00000000-1")).thenReturn(usuario(true));
        credencialesInvalidas(() -> servicio.iniciar(pasaporte, "00000000-1"));
        verify(usuarios).buscarPorDocumento(pasaporte, "00000000-1");
    }
    @Test void usuarioInactivoSeRechazaConElMismoMensaje() {
        when(usuarios.buscarPorDocumento(dui, "00000000-1")).thenReturn(usuario(false));
        credencialesInvalidas(() -> servicio.iniciar(dui, "00000000-1"));
        verify(usuarios, never()).asignaciones(any());
    }
    @Test void pacienteNoPuedeEntrarYRecibeElMismoMensaje() {
        when(usuarios.buscarPorDocumento(dui, "00000000-3")).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(asignacion("PACIENTE", 0)));
        credencialesInvalidas(() -> servicio.iniciar(dui, "00000000-3"));
    }
    @Test void personaSinAsignacionActivaRecibeElMismoMensaje() {
        when(usuarios.buscarPorDocumento(dui, "00000000-1")).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of());
        credencialesInvalidas(() -> servicio.iniciar(dui, "00000000-1"));
    }
    @Test void elRolInicialEsLaAsignacionDeEmpleadoMasAntiguaNuncaPaciente() {
        PersonaRol paciente = asignacion("PACIENTE", 0), doctor = asignacion("DOCTOR", 1), admin = asignacion("ADMINISTRACION", 2);
        Usuario u = usuario(true);
        when(usuarios.buscarPorDocumento(dui, "00000000-2")).thenReturn(u);
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(paciente, doctor, admin));
        var contexto = servicio.iniciar(dui, "00000000-2");
        assertSame(u, contexto.usuario()); assertSame(doctor, contexto.asignacion());
    }
    @Test void enfermeraEsEmpleadaYPuedeEntrar() {
        PersonaRol enfermera = asignacion("ENFERMERA", 0);
        when(usuarios.buscarPorDocumento(dui, "01000002-2")).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(enfermera));
        assertSame(enfermera, servicio.iniciar(dui, "01000002-2").asignacion());
    }
    @Test void unRolSinCodigoCreadoDesdeLaAppEsDeEmpleado() {
        PersonaRol sinCodigo = asignacion(null, 0);
        when(usuarios.buscarPorDocumento(dui, "01000003-3")).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(sinCodigo));
        assertSame(sinCodigo, servicio.iniciar(dui, "01000003-3").asignacion());
    }
    @Test void unRolInactivoNoEsDeEmpleadoAunqueLleguePorError() {
        PersonaRol farmaceutico = asignacion("FARMACEUTICO", 0); farmaceutico.getIdRol().setActivo(false);
        when(usuarios.buscarPorDocumento(dui, "01000005-5")).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(farmaceutico));
        credencialesInvalidas(() -> servicio.iniciar(dui, "01000005-5"));
    }
    @Test void esEmpleadoExcluyePacienteYRolesInactivos() {
        assertTrue(AutenticacionService.esEmpleado(asignacion("DOCTOR", 0)));
        assertTrue(AutenticacionService.esEmpleado(asignacion("ENFERMERA", 0)));
        assertTrue(AutenticacionService.esEmpleado(asignacion(null, 0)));
        assertFalse(AutenticacionService.esEmpleado(asignacion("PACIENTE", 0)));
        PersonaRol inactivo = asignacion("ENFERMERA", 0); inactivo.getIdRol().setActivo(false);
        assertFalse(AutenticacionService.esEmpleado(inactivo));
        assertFalse(AutenticacionService.esEmpleado(null));
        assertFalse(AutenticacionService.esEmpleado(new PersonaRol(UUID.randomUUID())));
    }
    @Test void numeroVacioOTipoNuloSeRechazanSinConsultar() {
        credencialesInvalidas(() -> servicio.iniciar(null, "00000000-1"));
        credencialesInvalidas(() -> servicio.iniciar(dui, "  "));
        verifyNoInteractions(usuarios);
    }
    @Test void elServicioYaNoDependeDeContrasenas() {
        assertTrue(Arrays.stream(AutenticacionService.class.getDeclaredFields()).noneMatch(f -> f.getType() == ContrasenaService.class));
    }

    // --- Sesión existente ---
    @Test void noAdmiteUnaAsignacionAjenaAunqueElClienteConozcaSuUuid() {
        when(usuarios.buscarSesion(idUsuario)).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(asignacion()));
        assertEquals(403, assertThrows(AccesoException.class, () -> servicio.contexto(idUsuario, UUID.randomUUID())).getStatus());
    }
    @Test void unaSesionNoPuedeTenerActivaUnaAsignacionDePaciente() {
        PersonaRol paciente = asignacion("PACIENTE", 0);
        when(usuarios.buscarSesion(idUsuario)).thenReturn(usuario(true));
        when(usuarios.asignaciones(idPersona)).thenReturn(List.of(paciente));
        assertEquals(403, assertThrows(AccesoException.class, () -> servicio.contexto(idUsuario, paciente.getIdPersonaRol())).getStatus());
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
