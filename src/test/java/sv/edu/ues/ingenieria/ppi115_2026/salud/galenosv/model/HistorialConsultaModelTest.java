package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistorialConsultaModelTest {
    @Mock AtencionService servicio;
    @Spy AtencionSesion sesion = new AtencionSesion();
    @InjectMocks HistorialConsultaModel model;

    @Test void crearConservaDatosYActivaElFlujoDeLaConsulta() {
        UUID paciente = UUID.randomUUID(); Consulta creada = new Consulta(UUID.randomUUID());
        model.setPacienteId(paciente.toString()); model.setReferencia("REF"); model.setObservaciones("Notas");
        when(servicio.crear(paciente, "REF", "Notas")).thenReturn(creada);
        assertEquals("/paginas/consulta.xhtml?faces-redirect=true", model.crear());
        assertEquals(creada.getIdConsulta(), sesion.getConsulta());
    }
    @Test void falloDeCreacionConservaFormularioYNoActivaConsulta() {
        UUID paciente = UUID.randomUUID(); model.setPacienteId(paciente.toString()); model.setReferencia("REF");
        doThrow(new ServiceException("Paciente de otra clínica")).when(servicio).crear(paciente, "REF", null);
        FacesContext faces = mock(FacesContext.class);
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            assertNull(model.crear()); assertNull(sesion.getConsulta()); assertEquals("REF", model.getReferencia());
            verify(faces).validationFailed();
        }
    }
    @Test void retomaConsultaAbiertaDespuesDeVerificarPropietario() {
        Consulta consulta = new Consulta(UUID.randomUUID()); when(servicio.cargar(consulta.getIdConsulta())).thenReturn(consulta);
        assertEquals("/paginas/consulta.xhtml?faces-redirect=true", model.retomar(consulta));
        assertEquals(consulta.getIdConsulta(), sesion.getConsulta()); verify(servicio).cargar(consulta.getIdConsulta());
    }
    @Test void noRetomaConsultaAjenaNiCerrada() {
        Consulta ajena = new Consulta(UUID.randomUUID()), cerrada = new Consulta(UUID.randomUUID());
        cerrada.setFechaFin(OffsetDateTime.now());
        when(servicio.cargar(ajena.getIdConsulta())).thenThrow(new ServiceException("Consulta ajena"));
        when(servicio.cargar(cerrada.getIdConsulta())).thenReturn(cerrada);
        FacesContext faces = mock(FacesContext.class);
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            assertNull(model.retomar(ajena)); assertNull(model.retomar(cerrada)); assertNull(sesion.getConsulta());
        }
    }
    @Test void cambiarDetalleLimpiaLaOrdenYElResultadoAnterior() {
        Consulta consulta = new Consulta(UUID.randomUUID()); when(servicio.cargar(consulta.getIdConsulta())).thenReturn(consulta);
        model.setOrdenId(UUID.randomUUID().toString()); model.setResultado("Anterior"); model.setInterpretacion("Anterior");
        model.seleccionar(consulta);
        assertSame(consulta, model.getSeleccionada()); assertNull(model.getOrdenId());
        assertNull(model.getResultado()); assertNull(model.getInterpretacion());
    }
    @Test void sinSesionNoCargaConsultasNiPacientes() {
        model.iniciar(); assertTrue(model.getConsultas().isEmpty()); assertTrue(model.getPacientes().isEmpty());
        verifyNoInteractions(servicio);
    }
    @Test void cambioDeSesionEnOtraPestanaLimpiaElHistorialYElDetalleAnterior() {
        Clinica clinica = new Clinica(UUID.randomUUID());
        Rol medico = new Rol(UUID.randomUUID()); medico.setActivo(true);
        PersonaRol primero = new PersonaRol(UUID.randomUUID()); primero.setIdPersona(new Persona(UUID.randomUUID()));
        primero.setIdClinica(clinica); primero.setIdRol(medico);
        PersonaRol segundo = new PersonaRol(UUID.randomUUID()); segundo.setIdPersona(new Persona(UUID.randomUUID()));
        segundo.setIdClinica(clinica); segundo.setIdRol(medico);
        Consulta anterior = new Consulta(UUID.randomUUID());
        sesion.cambiarRol(primero); when(servicio.historial()).thenReturn(List.of(anterior), List.of());
        model.iniciar(); when(servicio.cargar(anterior.getIdConsulta())).thenReturn(anterior); model.seleccionar(anterior);
        model.setPacienteId(UUID.randomUUID().toString()); model.setReferencia("Anterior");
        sesion.cambiarRol(segundo); model.verificarContexto();
        assertTrue(model.getConsultas().isEmpty()); assertNull(model.getSeleccionada());
        assertNull(model.getPacienteId()); assertNull(model.getReferencia()); verify(servicio, times(2)).historial();
    }
}
