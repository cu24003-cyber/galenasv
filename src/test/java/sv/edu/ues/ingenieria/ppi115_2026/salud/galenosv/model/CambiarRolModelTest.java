package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ContextoRolService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CambiarRolModelTest {
    @Mock ContextoRolService servicio;
    @Spy AtencionSesion sesion = new AtencionSesion();
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
    @Test void filtraPersonasYRolesPorClinicaSinDuplicarPersonas() {
        Clinica primera = clinica("Primera"), segunda = clinica("Segunda");
        Persona ana = new Persona(UUID.randomUUID()), luis = new Persona(UUID.randomUUID());
        PersonaRol medico = asignacion(primera, ana, "Médico"), enfermera = asignacion(primera, ana, "Enfermería");
        PersonaRol otro = asignacion(segunda, luis, "Médico");
        when(servicio.listar()).thenReturn(List.of(medico, enfermera, otro)); model.iniciar();
        assertEquals(List.of(primera, segunda), model.getClinicas()); assertTrue(model.getPersonas().isEmpty());
        model.setClinicaId(primera.getIdClinica().toString()); model.cambiarClinica();
        assertEquals(List.of(ana), model.getPersonas()); assertTrue(model.getRolesPersona().isEmpty());
        model.setPersonaId(ana.getIdPersona().toString()); model.cambiarPersona();
        assertEquals(List.of(medico, enfermera), model.getRolesPersona());
        assertEquals(medico.getIdPersonaRol().toString(), model.getAsignacionId());
        model.setAsignacionId(medico.getIdPersonaRol().toString());
        model.setClinicaId(segunda.getIdClinica().toString()); model.cambiarClinica();
        assertNull(model.getPersonaId()); assertNull(model.getAsignacionId()); assertEquals(List.of(luis), model.getPersonas());
        model.setPersonaId(luis.getIdPersona().toString()); model.cambiarPersona();
        assertEquals(otro.getIdPersonaRol().toString(), model.getAsignacionId());
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
        assertEquals("/paginas/consultas.xhtml?faces-redirect=true", model.aplicar()); assertSame(elegida, sesion.getRolActivo());
    }
    @Test void conservaElRolActualCuandoLaPersonaTieneVariasAsignaciones() {
        Clinica clinica = clinica("Actual"); Persona persona = new Persona(UUID.randomUUID());
        PersonaRol enfermeria = asignacion(clinica, persona, "Enfermería");
        PersonaRol medico = asignacion(clinica, persona, "Médico");
        sesion.cambiarRol(medico); when(servicio.listar()).thenReturn(List.of(enfermeria, medico)); model.iniciar();
        model.cambiarPersona(); assertEquals(medico.getIdPersonaRol().toString(), model.getAsignacionId());
    }
}
