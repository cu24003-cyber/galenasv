package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaModelTest {

    @Mock
    private PersonaService personaService;

    @Mock
    private FacesContext facesContext;

    @InjectMocks
    private PersonaModel model;

    private abstract static class Contexto extends FacesContext {
        static void establecer(FacesContext contexto) {
            setCurrentInstance(contexto);
        }
    }

    @AfterEach
    void limpiarContexto() {
        Contexto.establecer(null);
    }

    @Test
    void cancelarEdicionDescartaCambiosSinAlterarLaFila() {
        Persona original = new Persona(UUID.randomUUID());
        original.setNombres("Ana");
        original.setFechaNacimiento(new java.util.Date(1000));
        original.setFechaCreacion(new java.util.Date(2000));
        model.editar(original);
        assertNotSame(original, model.getSeleccionada());
        assertEquals(original.getFechaCreacion(), model.getSeleccionada().getFechaCreacion());
        model.getSeleccionada().setNombres("Otro nombre");
        model.getSeleccionada().getFechaNacimiento().setTime(3000);

        model.cancelar();

        assertNull(model.getSeleccionada());
        assertEquals("Ana", original.getNombres());
        assertEquals(1000, original.getFechaNacimiento().getTime());
        verifyNoInteractions(personaService);
    }

    @Test
    void init_cargaRegistrosDisponiblesParaLaVistaExistente() {
        List<Persona> personas = List.of(new Persona());
        when(personaService.listarTodos()).thenReturn(personas);

        model.init();

        assertSame(personas, model.getPersonas());
        assertSame(personas, model.getRegistros());
    }

    @Test
    void guardar_creaUnaPersonaNuevaYRecargaLaLista() {
        model.nuevo();
        Persona nueva = model.getSeleccionada();
        when(personaService.listarTodos()).thenReturn(List.of(nueva));

        model.guardar();

        verify(personaService).crear(nueva);
        verify(personaService, never()).actualizar(any());
        assertNull(model.getSeleccionada());
        assertEquals(List.of(nueva), model.getPersonas());
    }

    @Test
    void guardar_actualizaUnaPersonaExistente() {
        Persona persona = new Persona();
        persona.setIdPersona(UUID.randomUUID());
        model.editar(persona);

        model.guardar();

        verify(personaService).actualizar(persona);
        verify(personaService, never()).crear(any());
        assertNull(model.getSeleccionada());
    }

    @Test
    void guardar_conErrorConservaLaSeleccionYMarcaValidacionFallida() {
        Contexto.establecer(facesContext);
        model.nuevo();
        Persona persona = model.getSeleccionada();
        doThrow(new ServiceException("No se pudo guardar")).when(personaService).crear(persona);

        model.guardar();

        assertSame(persona, model.getSeleccionada());
        verify(facesContext).validationFailed();
        verify(facesContext).addMessage(isNull(), any());
        verify(personaService, never()).listarTodos();
    }

    @Test
    void guardar_sinSeleccionNoInvocaElServicio() {
        Contexto.establecer(facesContext);

        model.guardar();

        verifyNoInteractions(personaService);
        verify(facesContext).validationFailed();
    }

    @Test
    void eliminar_delegaAlServicioYRecargaRegistros() {
        Persona persona = new Persona();
        persona.setIdPersona(UUID.randomUUID());
        when(personaService.listarTodos()).thenReturn(List.of());

        model.eliminar(persona);

        verify(personaService).eliminar(persona.getIdPersona());
        verify(personaService).listarTodos();
        assertTrue(model.getPersonas().isEmpty());
    }

    @Test
    void eliminar_conHistorialConservaLaFilaYMuestraElMotivoTraducido() {
        Contexto.establecer(facesContext);
        Persona persona = new Persona(UUID.randomUUID());
        List<Persona> personas = List.of(persona);
        when(personaService.listarTodos()).thenReturn(personas);
        model.init();
        doThrow(new ServiceException("persona.eliminar.historial", "Diagnóstico interno", null))
                .when(personaService).eliminar(persona.getIdPersona());

        model.eliminar(persona);

        assertSame(personas, model.getPersonas());
        verify(personaService, times(1)).listarTodos();
        verify(facesContext).validationFailed();
        var mensaje = org.mockito.ArgumentCaptor.forClass(jakarta.faces.application.FacesMessage.class);
        verify(facesContext).addMessage(isNull(), mensaje.capture());
        assertEquals(Mensajes.texto("persona.eliminar.historial"), mensaje.getValue().getDetail());
    }

    @Test
    void cargarPersonas_conErrorConservaLaListaAnterior() {
        Contexto.establecer(facesContext);
        List<Persona> personas = List.of(new Persona());
        when(personaService.listarTodos()).thenReturn(personas)
                .thenThrow(new ServiceException("No se pudo cargar"));
        model.init();

        assertDoesNotThrow(model::cargarPersonas);

        assertSame(personas, model.getPersonas());
    }
}
