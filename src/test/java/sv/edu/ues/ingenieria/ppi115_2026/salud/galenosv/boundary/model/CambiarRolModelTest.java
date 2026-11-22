package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AutenticacionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ContextoRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.IntentosAcceso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import jakarta.faces.application.FacesMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

@ExtendWith(MockitoExtension.class)
class CambiarRolModelTest {
    @Mock ContextoRolService servicio;
    @Spy AtencionSesion sesion = new AtencionSesion();
    @Spy AccesoSesion acceso = new AccesoSesion();
    @Mock AutenticacionService autenticacion;
    @Mock IntentosAcceso intentos;
    @Mock TipoDocumentoService tipos;
    @InjectMocks CambiarRolModel model;

    private PersonaRol asignacion(Clinica clinica, Persona persona, String nombreRol) {
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre(nombreRol); rol.setActivo(true);
        PersonaRol asignacion = new PersonaRol(UUID.randomUUID());
        asignacion.setIdClinica(clinica); asignacion.setIdPersona(persona); asignacion.setIdRol(rol);
        return asignacion;
    }
    private Clinica clinica(String nombre) {
        Clinica clinica = new Clinica(UUID.randomUUID()); clinica.setNombre(nombre); return clinica;
    }
    @Test void soloOfreceAsignacionesDisponiblesDeLaSesion() {
        Clinica primera = clinica("Primera"), segunda = clinica("Segunda");
        Persona ana = new Persona(UUID.randomUUID());
        PersonaRol medico = asignacion(primera, ana, "Médico"), enfermera = asignacion(primera, ana, "Enfermería");
        when(servicio.listar()).thenReturn(List.of(medico, enfermera)); model.iniciar();
        assertEquals(List.of(primera), model.getClinicas()); assertTrue(model.getPersonas().isEmpty());
        model.setClinicaId(primera.getIdClinica().toString()); model.cambiarClinica();
        assertEquals(List.of(ana), model.getPersonas()); assertEquals(List.of(medico, enfermera), model.getRolesPersona());
        model.setPersonaId(ana.getIdPersona().toString()); model.cambiarPersona();
        assertEquals(List.of(medico, enfermera), model.getRolesPersona());
        assertEquals(medico.getIdPersonaRol().toString(), model.getAsignacionId());
        model.setAsignacionId(medico.getIdPersonaRol().toString());
        model.setClinicaId(segunda.getIdClinica().toString()); model.cambiarClinica();
        assertNull(model.getPersonaId()); assertNull(model.getAsignacionId()); assertTrue(model.getPersonas().isEmpty());
    }
    @Test void iniciaConLaClinicaPersonaYRolDeLaSesion() {
        PersonaRol actual = asignacion(clinica("Actual"), new Persona(UUID.randomUUID()), "Médico");
        sesion.cambiarRol(actual); when(servicio.listar()).thenReturn(List.of(actual)); model.iniciar();
        assertEquals(actual.getIdClinica().getIdClinica().toString(), model.getClinicaId());
        assertEquals(actual.getIdPersona().getIdPersona().toString(), model.getPersonaId());
        assertEquals(actual.getIdPersonaRol().toString(), model.getAsignacionId());
    }
    @Test void rechazaAsignacionQueNoCoincideConLaClinicaSeleccionada() {
        PersonaRol otra = asignacion(clinica("Otra"), new Persona(UUID.randomUUID()), "Médico");
        when(servicio.listar()).thenReturn(List.of(otra)); model.iniciar();
        model.setClinicaId(UUID.randomUUID().toString()); model.setPersonaId(otra.getIdPersona().getIdPersona().toString());
        model.setAsignacionId(otra.getIdPersonaRol().toString());
        FacesContext faces = mock(FacesContext.class);
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            assertNull(model.aplicar()); assertNull(sesion.getRolActivo()); verify(faces).validationFailed();
        }
    }
    @Test void aplicaAsignacionVerificadaYNavegaAConsultas() {
        PersonaRol elegida = asignacion(clinica("Actual"), new Persona(UUID.randomUUID()), "Médico");
        when(servicio.listar()).thenReturn(List.of(elegida)); model.iniciar();
        model.setClinicaId(elegida.getIdClinica().getIdClinica().toString());
        model.setPersonaId(elegida.getIdPersona().getIdPersona().toString());
        when(servicio.cargar(elegida.getIdPersonaRol())).thenReturn(elegida);
        FacesContext faces = mock(FacesContext.class);
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            ExternalContext externo = mock(ExternalContext.class); Flash flash = mock(Flash.class);
            when(faces.getExternalContext()).thenReturn(externo); when(externo.getFlash()).thenReturn(flash);
            assertEquals("/paginas/consultas.xhtml?faces-redirect=true", model.aplicar()); assertSame(elegida, sesion.getRolActivo());
            verify(flash).setKeepMessages(true);
        }
    }
    @Test void conservaElRolActualCuandoLaPersonaTieneVariasAsignaciones() {
        Clinica clinica = clinica("Actual"); Persona persona = new Persona(UUID.randomUUID());
        PersonaRol enfermeria = asignacion(clinica, persona, "Enfermería");
        PersonaRol medico = asignacion(clinica, persona, "Médico");
        sesion.cambiarRol(medico); when(servicio.listar()).thenReturn(List.of(enfermeria, medico)); model.iniciar();
        model.cambiarPersona(); assertEquals(medico.getIdPersonaRol().toString(), model.getAsignacionId());
    }

    @Test void rechazaPersonaAjenaAunqueSeEnvieEnElFormulario() {
        Persona propia = new Persona(UUID.randomUUID()), ajena = new Persona(UUID.randomUUID());
        PersonaRol asignacionAjena = asignacion(clinica("Clínica"), ajena, "Médico");
        when(servicio.listar()).thenReturn(List.of()); model.iniciar();
        model.setClinicaId(asignacionAjena.getIdClinica().getIdClinica().toString());
        model.setPersonaId(propia.getIdPersona().toString());
        model.setAsignacionId(asignacionAjena.getIdPersonaRol().toString());
        FacesContext faces = mock(FacesContext.class);
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            assertNull(model.aplicar()); assertNull(sesion.getRolActivo()); verify(faces).validationFailed();
        }
    }

    // --- Entrada desde la misma pantalla cuando no hay sesión (HU2 en JSF) ---

    private HttpServletRequest peticion() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        lenient().when(request.getRemoteAddr()).thenReturn("10.0.0.7");
        lenient().when(request.getSession(true)).thenReturn(mock(HttpSession.class));
        return request;
    }

    private String mensajeDeError(Runnable accion) {
        FacesContext faces = mock(FacesContext.class);
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            accion.run();
            ArgumentCaptor<FacesMessage> mensaje = ArgumentCaptor.forClass(FacesMessage.class);
            verify(faces).addMessage(isNull(), mensaje.capture()); verify(faces).validationFailed();
            assertEquals(FacesMessage.SEVERITY_ERROR, mensaje.getValue().getSeverity());
            return mensaje.getValue().getSummary();
        }
    }

    @Test void sinSesionMuestraElFormularioConLosTiposDeDocumentoActivos() {
        TipoDocumento dui = new TipoDocumento(UUID.randomUUID()); dui.setNombre("DUI");
        when(tipos.listarActivos()).thenReturn(List.of(dui));
        assertFalse(model.isAutenticado());
        assertEquals(List.of(dui), model.getTiposDocumento());
        acceso.autenticar(UUID.randomUUID());
        assertTrue(model.isAutenticado());
    }

    @Test void entrarConDocumentoDeEmpleadoUsaElMismoServicioCambiaElIdDeSesionYDejaSuRol() {
        UUID tipo = UUID.randomUUID();
        Persona doctor = new Persona(UUID.randomUUID());
        PersonaRol suRol = asignacion(clinica("Central"), doctor, "Doctor");
        PersonaRol anterior = asignacion(clinica("Otra"), new Persona(UUID.randomUUID()), "Enfermera");
        sesion.cambiarRol(anterior); sesion.setConsulta(UUID.randomUUID());
        when(autenticacion.iniciar(tipo, " 02345678-9 ")).thenReturn(new AutenticacionService.Contexto(doctor, suRol));
        HttpServletRequest request = peticion();
        model.setTipoDocumentoId(tipo.toString()); model.setNumeroDocumento(" 02345678-9 ");

        assertEquals("/paginas/cambiar-rol.xhtml?faces-redirect=true", model.entrar(request));

        InOrder orden = inOrder(intentos, autenticacion, request, acceso);
        orden.verify(intentos).registrar("10.0.0.7");
        orden.verify(autenticacion).iniciar(tipo, " 02345678-9 ");
        orden.verify(request).getSession(true);
        orden.verify(request).changeSessionId();
        orden.verify(acceso).autenticar(doctor.getIdPersona());
        assertTrue(acceso.vigente());
        assertSame(suRol, sesion.getRolActivo());
        assertNull(sesion.getConsulta());
    }

    @Test void pacienteOPersonaSinRolNoEntranYLaSesionNoCambia() {
        UUID tipo = UUID.randomUUID();
        when(autenticacion.iniciar(any(), any())).thenThrow(new AccesoException(401, "auth.credenciales", "x"));
        HttpServletRequest request = peticion();
        model.setTipoDocumentoId(tipo.toString()); model.setNumeroDocumento("01234567-8");
        String texto = mensajeDeError(() -> assertNull(model.entrar(request)));
        assertEquals("No encontramos una persona con ese documento activo.", texto);
        verify(intentos).registrar("10.0.0.7");
        verify(request, never()).changeSessionId();
        assertFalse(acceso.vigente()); assertNull(sesion.getRolActivo());
    }

    @Test void respetaElMismoLimiteDeIntentosQueElApi() {
        doThrow(new AccesoException(429, "auth.demasiadosIntentos", "x")).when(intentos).registrar("10.0.0.7");
        HttpServletRequest request = peticion();
        model.setTipoDocumentoId(UUID.randomUUID().toString()); model.setNumeroDocumento("02345678-9");
        assertEquals("Espere antes de volver a iniciar sesión.", mensajeDeError(() -> assertNull(model.entrar(request))));
        verifyNoInteractions(autenticacion);
        verify(request, never()).changeSessionId();
    }

    @Test void datosIncompletosOTipoInvalidoNoConsultanNiCuentanIntento() {
        HttpServletRequest request = peticion();
        model.setTipoDocumentoId(""); model.setNumeroDocumento("02345678-9");
        assertEquals("Seleccione el tipo de documento y escriba su número.", mensajeDeError(() -> assertNull(model.entrar(request))));
        model.setTipoDocumentoId("no-es-uuid");
        mensajeDeError(() -> assertNull(model.entrar(request)));
        verifyNoInteractions(autenticacion, intentos);
        assertFalse(acceso.vigente());
    }
}
