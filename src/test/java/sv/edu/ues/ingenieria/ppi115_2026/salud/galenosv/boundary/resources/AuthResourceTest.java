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
    private final UUID idUsuario = UUID.randomUUID();

    @BeforeEach void preparar() {
        resource = new AuthResource(); acceso = new AccesoSesion(); atencion = new AtencionSesion();
        resource.servicio = servicio; resource.request = request; resource.intentos = intentos;
        resource.acceso = acceso; resource.atencion = atencion;
    }
    private AutenticacionService.Contexto contexto(String codigo) {
        Persona persona = new Persona(UUID.randomUUID()); persona.setNombres("Julio"); persona.setApellidos("Prueba");
        Usuario usuario = new Usuario(); usuario.setIdUsuario(idUsuario); usuario.setPersona(persona); usuario.setPasswordHash("NO-DEBE-SALIR");
        Rol rol = new Rol(UUID.randomUUID()); rol.setCodigo(codigo); rol.setActivo(true);
        Clinica clinica = new Clinica(UUID.randomUUID()); clinica.setNombre("Clínica de prueba"); clinica.setActivo(true);
        PersonaRol asignacion = new PersonaRol(UUID.randomUUID()); asignacion.setIdPersona(persona); asignacion.setIdRol(rol); asignacion.setIdClinica(clinica);
        return new AutenticacionService.Contexto(usuario, asignacion);
    }
    @Test void loginRenuevaLaSesionYSoloGuardaIdentidadYContexto() {
        var contexto = contexto("ADMINISTRACION"); when(servicio.iniciar("julio", " prueba ")).thenReturn(contexto);
        when(request.getSession(false)).thenReturn(http); when(request.getSession(true)).thenReturn(http);
        var datos = new AuthResource.Credenciales(); datos.identificador = "julio"; datos.password = " prueba ";
        assertEquals(204, resource.login(datos).getStatus()); verify(http).invalidate(); verify(request).changeSessionId();
        assertTrue(acceso.vigente()); assertEquals(idUsuario, acceso.getIdUsuario());
        assertSame(contexto.asignacion(), atencion.getRolActivo()); assertNotNull(acceso.getCsrfToken());
    }
    @Test void credencialesInvalidasNoCreanNiReemplazanUnaSesion() {
        when(servicio.iniciar(any(), any())).thenThrow(new AccesoException(401,"auth.credenciales","Usuario o contraseña incorrectos."));
        var datos = new AuthResource.Credenciales(); datos.identificador = "julio"; datos.password = "incorrecta";
        assertThrows(AccesoException.class, () -> resource.login(datos));
        verify(request, never()).getSession(anyBoolean()); assertFalse(acceso.vigente()); assertNull(atencion.getRolActivo());
    }
    @Test void meDevuelveLosTresRolesSinHashNiCorreoYConElContratoDelFrontend() {
        when(request.getSession(false)).thenReturn(http); acceso.autenticar(idUsuario);
        for (String codigo : new String[]{"ADMINISTRACION", "DOCTOR", "PACIENTE"}) {
            var contexto = contexto(codigo); atencion.cambiarRol(contexto.asignacion());
            when(servicio.contexto(idUsuario, contexto.asignacion().getIdPersonaRol())).thenReturn(contexto);
            String body = (String) resource.me().getEntity();
            var json = Json.createReader(new StringReader(body)).readObject();
            assertEquals(codigo, json.getJsonObject("rolActivo").getString("codigo"));
            assertEquals(idUsuario.toString(), json.getJsonObject("usuario").getString("idUsuario"));
            assertEquals(acceso.getCsrfToken(), json.getString("csrfToken"));
            assertFalse(body.contains("NO-DEBE-SALIR")); assertFalse(body.contains("passwordHash")); assertFalse(body.contains("correo"));
        }
    }
    @Test void unaAsignacionRevocadaInvalidaLaSesionAnterior() {
        when(request.getSession(false)).thenReturn(http); acceso.autenticar(idUsuario);
        when(servicio.contexto(idUsuario, null)).thenThrow(new AccesoException(403, "auth.asignacion", "Acceso revocado."));
        assertEquals(403, assertThrows(AccesoException.class, () -> resource.me()).getStatus());
        verify(http).invalidate();
    }
    @Test void logoutInvalidaTodaLaSesion() {
        when(request.getSession(false)).thenReturn(http); acceso.autenticar(idUsuario);
        assertEquals(204, resource.logout().getStatus()); verify(http).invalidate();
    }
    @Test void sinCookieNoHaySesionActualNiLogout() {
        assertEquals(401, assertThrows(AccesoException.class, () -> resource.me()).getStatus());
        assertEquals(401, assertThrows(AccesoException.class, () -> resource.logout()).getStatus());
    }
}
