package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaBusquedaTest {
    @Mock PersonaService service;
    @InjectMocks PersonaModel model;

    @Test
    void buscar_filtraYRegresaALaPrimeraPagina() {
        List<Persona> personas = List.of(new Persona());
        when(service.buscarPorNombre("Ana")).thenReturn(personas);
        model.setPrimeraFila(20);
        model.setNombreBusqueda("Ana");

        model.cargarPersonas();

        assertSame(personas, model.getPersonas());
        assertEquals(0, model.getPrimeraFila());
        verify(service, never()).listarTodos();
    }

    @Test
    void borrarFiltro_recuperaTodasLasPersonas() {
        when(service.buscarPorNombre("Ana")).thenReturn(List.of());
        List<Persona> todas = List.of(new Persona());
        when(service.listarTodos()).thenReturn(todas);
        model.setNombreBusqueda("Ana");
        model.cargarPersonas();
        model.setNombreBusqueda("");

        model.cargarPersonas();

        assertSame(todas, model.getPersonas());
    }

    @Test
    void guardarYEliminar_conservanElFiltro() {
        model.setNombreBusqueda("Ana");
        when(service.buscarPorNombre("Ana")).thenReturn(List.of());
        model.nuevo();
        Persona persona = model.getSeleccionada();
        persona.setNombres("Ana");
        model.guardar();
        persona.setIdPersona(UUID.randomUUID());
        model.eliminar(persona);

        assertEquals("Ana", model.getNombreBusqueda());
        assertTrue(model.getPersonas().isEmpty());
        verify(service).crear(persona);
        verify(service).eliminar(persona.getIdPersona());
        verify(service, never()).listarTodos();
    }
}
