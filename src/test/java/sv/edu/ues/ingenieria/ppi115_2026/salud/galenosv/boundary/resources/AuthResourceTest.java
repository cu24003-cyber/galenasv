package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.json.Json;
import jakarta.servlet.http.*;
import java.io.StringReader;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {
    @Mock AutenticacionService servicio;
    @Mock HttpServletRequest request;
    @Mock HttpSession http;
    @Mock IntentosAcceso intentos;
    private AuthResource resource;
    private AccesoSesion acceso;
    private AtencionSesion atencion;
    private final UUID idPersona = UUID.randomUUID();

    @BeforeEach void preparar() {
        resource = new AuthResource(); acceso = new AccesoSesion(); atencion = new AtencionSesion();
        resource.servicio = servicio; resource.request = request; resource.intentos = intentos;
        resource.acceso = acceso; resource.atencion = atencion;
    }
    private AutenticacionService.Contexto contexto(String codigo) {
        Persona persona = new Persona(idPersona); persona.setNombres("Julio"); persona.setApellidos("Prueba");
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre("ADMINISTRACION".equals(codigo) ? "Atención al público" : codigo); rol.setActivo(true);
        Clinica clinica = new Clinica(UUID.randomUUID()); clinica.setNombre("Clínica de prueba"); clinica.setActivo(true);
        PersonaRol asignacion = new PersonaRol(UUID.randomUUID()); asignacion.setIdPersona(persona); asignacion.setIdRol(rol); asignacion.setIdClinica(clinica);
        return new AutenticacionService.Contexto(persona, asignacion);
    }
    private AuthResource.Credenciales credenciales(UUID tipo, String numero) {
        var datos = new AuthResource.Credenciales(); datos.idTipoDocumento = tipo; datos.numeroDocumento = numero; return datos;
    }
    @Test void loginPorDocumentoRenuevaLaSesionYSoloGuardaIdentidadYContexto() {
        UUID dui = UUID.randomUUID(); var contexto = contexto("ADMINISTRACION");
        when(servicio.iniciar(dui, "00000000-1")).thenReturn(contexto);
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        when(request.getSession(false)).thenReturn(http); when(request.getSession(true)).thenReturn(http);
        assertEquals(204, resource.login(credenciales(dui, "00000000-1")).getStatus());
        verify(intentos).registrar("192.0.2.10"); verify(http).invalidate(); verify(request).changeSessionId();
        assertTrue(acceso.vigente()); assertEquals(idPersona, acceso.getIdPersona());
        assertSame(contexto.asignacion(), atencion.getRolActivo()); assertNotNull(acceso.getCsrfToken());
    }
    @Test void documentoRechazadoCuentaElIntentoPeroNoCreaNiReemplazaSesion() {
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        when(servicio.iniciar(any(UUID.class), anyString())).thenThrow(new AccesoException(401,"auth.credenciales","No encontramos una persona con ese documento activo."));
        assertEquals(401, assertThrows(AccesoException.class, () -> resource.login(credenciales(UUID.randomUUID(), "00000000-9"))).getStatus());
        verify(intentos).registrar("192.0.2.10");
        verify(request, never()).getSession(anyBoolean()); assertFalse(acceso.vigente()); assertNull(atencion.getRolActivo());
    }
    @Test void personaSinAsignacionEsRechazadaAntesDeCrearSesion() {
        UUID dui = UUID.randomUUID();
        when(servicio.iniciar(dui, "00000000-5"))
                .thenThrow(new AccesoException(401, "auth.credenciales", "No encontramos una persona con ese documento activo."));
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        var error = assertThrows(AccesoException.class, () -> resource.login(credenciales(dui, "00000000-5")));
        assertEquals(401, error.getStatus()); assertEquals("auth.credenciales", error.getMessageKey());
        verify(request, never()).getSession(anyBoolean()); assertFalse(acceso.vigente());
    }
    @Test void demasiadosIntentosNoLleganAlServicio() {
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        doThrow(new AccesoException(429, "auth.demasiadosIntentos", "Espere.")).when(intentos).registrar("192.0.2.10");
        assertEquals(429, assertThrows(AccesoException.class, () -> resource.login(credenciales(UUID.randomUUID(), "00000000-1"))).getStatus());
        verifyNoInteractions(servicio);
    }
    @Test void elCuerpoDelLoginNoTieneContrasenaYValidaTipoYNumero() {
        assertThrows(NoSuchFieldException.class, () -> AuthResource.Credenciales.class.getField("password"));
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validador = factory.getValidator();
            assertTrue(validador.validate(credenciales(UUID.randomUUID(), "00000000-1")).isEmpty());
            assertFalse(validador.validate(credenciales(null, "00000000-1")).isEmpty());
            assertFalse(validador.validate(credenciales(UUID.randomUUID(), " ")).isEmpty());
            assertFalse(validador.validate(credenciales(UUID.randomUUID(), "1".repeat(51))).isEmpty());
            assertFalse(validador.validate(credenciales(UUID.randomUUID(), "00000000-1\r\nX")).isEmpty());
        }
    }
    @Test void meDevuelveLosRolesDeEmpleadoSinHashNiCorreoYConElContratoDelFrontend() {
        when(request.getSession(false)).thenReturn(http); acceso.autenticar(idPersona);
        for (String codigo : new String[]{"ADMINISTRACION", "DOCTOR"}) {
            var contexto = contexto(codigo); atencion.cambiarRol(contexto.asignacion());
            when(servicio.personaActiva(idPersona)).thenReturn(contexto.persona());
            when(servicio.contexto(idPersona, contexto.asignacion().getIdPersonaRol())).thenReturn(contexto);
            String body = (String) resource.me().getEntity();
            var json = Json.createReader(new StringReader(body)).readObject();
            assertEquals(codigo, json.getJsonObject("rolActivo").getString("codigo"));
            assertEquals(idPersona.toString(), json.getJsonObject("usuario").getString("idUsuario"));
            assertEquals(acceso.getCsrfToken(), json.getString("csrfToken"));
            assertFalse(body.contains("NO-DEBE-SALIR")); assertFalse(body.contains("passwordHash")); assertFalse(body.contains("correo"));
        }
    }
    @Test void meConEnfermeraDevuelveSuCodigoYElNombreDelRol() {
        when(request.getSession(false)).thenReturn(http); acceso.autenticar(idPersona);
        var contexto = contexto("ENFERMERA"); contexto.asignacion().getIdRol().setNombre("Enfermera");
        atencion.cambiarRol(contexto.asignacion());
        when(servicio.personaActiva(idPersona)).thenReturn(contexto.persona());
        when(servicio.contexto(idPersona, contexto.asignacion().getIdPersonaRol())).thenReturn(contexto);
        var rol = Json.createReader(new StringReader((String) resource.me().getEntity())).readObject().getJsonObject("rolActivo");
        assertEquals("ENFERMERA", rol.getString("codigo"));
        assertEquals("Enfermera", rol.getString("nombreRol"));
        assertEquals(contexto.asignacion().getIdRol().getIdRol().toString(), rol.getString("idRol"));
        assertEquals(java.util.Set.of("idPersonaRol", "idRol", "codigo", "nombreRol", "idClinica", "nombreClinica"), rol.keySet());
    }
    @Test void meConUnRolSinCodigoDevuelveCodigoNullEnLugarDeFallar() {
        when(request.getSession(false)).thenReturn(http); acceso.autenticar(idPersona);
        var contexto = contexto(null); contexto.asignacion().getIdRol().setNombre("Laboratorista");
        atencion.cambiarRol(contexto.asignacion());
        when(servicio.personaActiva(idPersona)).thenReturn(contexto.persona());
        when(servicio.contexto(idPersona, contexto.asignacion().getIdPersonaRol())).thenReturn(contexto);
        var rol = Json.createReader(new StringReader((String) resource.me().getEntity())).readObject().getJsonObject("rolActivo");
        assertTrue(rol.isNull("codigo"));
        assertEquals("Laboratorista", rol.getString("nombreRol"));
    }
    @Test void unaAsignacionRevocadaInvalidaLaSesionAnterior() {
        when(request.getSession(false)).thenReturn(http); acceso.autenticar(idPersona);
        var contexto = contexto("DOCTOR"); atencion.cambiarRol(contexto.asignacion());
        when(servicio.personaActiva(idPersona)).thenReturn(contexto.persona());
        when(servicio.contexto(idPersona, contexto.asignacion().getIdPersonaRol()))
                .thenThrow(new AccesoException(403, "auth.asignacion", "Acceso revocado."));
        assertEquals(403, assertThrows(AccesoException.class, () -> resource.me()).getStatus());
        verify(http).invalidate();
    }
    @Test void logoutInvalidaTodaLaSesion() {
        when(request.getSession(false)).thenReturn(http); acceso.autenticar(idPersona);
        assertEquals(204, resource.logout().getStatus()); verify(http).invalidate();
    }
    @Test void sinCookieNoHaySesionActualNiLogout() {
        assertEquals(401, assertThrows(AccesoException.class, () -> resource.me()).getStatus());
        assertEquals(401, assertThrows(AccesoException.class, () -> resource.logout()).getStatus());
    }
}
