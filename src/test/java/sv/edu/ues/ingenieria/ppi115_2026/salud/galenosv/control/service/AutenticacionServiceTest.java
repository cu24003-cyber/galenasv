package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.*;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {
    @Mock DocumentoRepository documentos;
    @Mock PersonaRepository personas;
    @Mock PersonaRolRepository roles;
    @InjectMocks AutenticacionService servicio;
    private final UUID idPersona = UUID.randomUUID(), tipo = UUID.randomUUID();

    private Persona persona() {
        Persona p = new Persona(idPersona); p.setNombres("Ana"); p.setApellidos("Prueba"); return p;
    }
    private PersonaRol asignacion(String codigo, int dias) {
        PersonaRol r = new PersonaRol(UUID.randomUUID()); r.setIdPersona(persona());
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre("ADMINISTRACION".equals(codigo) ? "Atención al público" : codigo); rol.setActivo(true); r.setIdRol(rol);
        Clinica clinica = new Clinica(UUID.randomUUID()); clinica.setActivo(true); r.setIdClinica(clinica);
        r.setFechaCreacion(Date.from(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(86400L * dias)));
        return r;
    }
    private void identidad(UUID tipoId, String valor) {
        when(documentos.buscarPersonaPorDocumento(tipoId, valor)).thenReturn(persona());
        when(roles.listarPorPersona(idPersona)).thenReturn(List.of());
    }

    @Test void documentoInexistenteSeRechazaSinConsultarRoles() {
        assertEquals(401, assertThrows(AccesoException.class,
                () -> servicio.iniciar(tipo, "00000000-9")).getStatus());
        verify(documentos).buscarPersonaPorDocumento(tipo, "00000000-9");
        verifyNoInteractions(roles);
    }
    @Test void personaSinAsignacionEmpleadoNoPuedeIniciar() {
        identidad(tipo, "00000000-1");
        var error = assertThrows(AccesoException.class, () -> servicio.iniciar(tipo, "00000000-1"));
        assertEquals(401, error.getStatus()); assertEquals("auth.credenciales", error.getMessageKey());
    }
    @Test void pacienteNoPuedeIniciarComoEmpleado() {
        PersonaRol paciente = asignacion("PACIENTE", 0);
        when(documentos.buscarPersonaPorDocumento(tipo, "00000000-2")).thenReturn(persona());
        when(roles.listarPorPersona(idPersona)).thenReturn(List.of(paciente));
        var error = assertThrows(AccesoException.class, () -> servicio.iniciar(tipo, "00000000-2"));
        assertEquals(401, error.getStatus()); assertEquals("auth.credenciales", error.getMessageKey());
    }
    @Test void rolInicialPrefiereLaAsignacionDeEmpleadoMasAntigua() {
        PersonaRol paciente = asignacion("PACIENTE", 0), doctor = asignacion("DOCTOR", 1), admin = asignacion("ADMINISTRACION", 2);
        when(documentos.buscarPersonaPorDocumento(tipo, "00000000-3")).thenReturn(persona());
        when(roles.listarPorPersona(idPersona)).thenReturn(List.of(paciente, doctor, admin));
        assertSame(doctor, servicio.iniciar(tipo, "00000000-3").asignacion());
    }
    @Test void rolDeEmpleadoSinCodigoPermiteIniciar() {
        PersonaRol rol = asignacion(null, 0);
        when(documentos.buscarPersonaPorDocumento(tipo, "00000000-6")).thenReturn(persona());
        when(roles.listarPorPersona(idPersona)).thenReturn(List.of(rol));
        assertSame(rol, servicio.iniciar(tipo, "00000000-6").asignacion());
    }
    @Test void asignacionInactivaNoPermiteIniciar() {
        PersonaRol inactiva = asignacion("DOCTOR", 0); inactiva.getIdRol().setActivo(false);
        when(documentos.buscarPersonaPorDocumento(tipo, "00000000-4")).thenReturn(persona());
        when(roles.listarPorPersona(idPersona)).thenReturn(List.of(inactiva));
        var error = assertThrows(AccesoException.class, () -> servicio.iniciar(tipo, "00000000-4"));
        assertEquals(401, error.getStatus()); assertEquals("auth.credenciales", error.getMessageKey());
    }
    @Test void clinicaInactivaNoPermiteIniciar() {
        PersonaRol asignacion = asignacion("DOCTOR", 0); asignacion.getIdClinica().setActivo(false);
        when(documentos.buscarPersonaPorDocumento(tipo, "00000000-7")).thenReturn(persona());
        when(roles.listarPorPersona(idPersona)).thenReturn(List.of(asignacion));
        var error = assertThrows(AccesoException.class, () -> servicio.iniciar(tipo, "00000000-7"));
        assertEquals(401, error.getStatus()); assertEquals("auth.credenciales", error.getMessageKey());
    }
    @Test void tipoNuloONumeroVacioSeRechazanSinConsultar() {
        assertEquals(401, assertThrows(AccesoException.class, () -> servicio.iniciar(null, "00000000-1")).getStatus());
        assertEquals(401, assertThrows(AccesoException.class, () -> servicio.iniciar(tipo, "  ")).getStatus());
        verifyNoInteractions(documentos, roles);
    }
    @Test void esEmpleadoExcluyePacienteYRolesInactivos() {
        assertTrue(AutenticacionService.esEmpleado(asignacion("DOCTOR", 0)));
        assertTrue(AutenticacionService.esEmpleado(asignacion("ENFERMERA", 0)));
        assertTrue(AutenticacionService.esEmpleado(asignacion(null, 0)));
        assertFalse(AutenticacionService.esEmpleado(asignacion("PACIENTE", 0)));
        PersonaRol inactivo = asignacion("ENFERMERA", 0); inactivo.getIdRol().setActivo(false);
        assertFalse(AutenticacionService.esEmpleado(inactivo));
        assertFalse(AutenticacionService.esEmpleado(null));
    }
    @Test void personaSesionDebeExistirYTenerAlgunTipoDeDocumentoActivo() {
        when(personas.find(idPersona)).thenReturn(persona());
        when(documentos.tieneDocumentoActivo(idPersona)).thenReturn(true);
        assertEquals(idPersona, servicio.personaActiva(idPersona).getIdPersona());
        when(documentos.tieneDocumentoActivo(idPersona)).thenReturn(false);
        assertEquals(401, assertThrows(AccesoException.class, () -> servicio.personaActiva(idPersona)).getStatus());
        when(personas.find(idPersona)).thenReturn(null);
        assertEquals(401, assertThrows(AccesoException.class, () -> servicio.personaActiva(idPersona)).getStatus());
    }
    @Test void contextoPermitePacientePeroNoAsignacionesAjenas() {
        PersonaRol paciente = asignacion("PACIENTE", 0);
        when(personas.find(idPersona)).thenReturn(persona());
        when(documentos.tieneDocumentoActivo(idPersona)).thenReturn(true);
        when(roles.listarPorPersona(idPersona)).thenReturn(List.of(paciente));
        assertSame(paciente, servicio.contexto(idPersona, paciente.getIdPersonaRol()).asignacion());
        assertEquals(403, assertThrows(AccesoException.class,
                () -> servicio.contexto(idPersona, UUID.randomUUID())).getStatus());
    }
}
