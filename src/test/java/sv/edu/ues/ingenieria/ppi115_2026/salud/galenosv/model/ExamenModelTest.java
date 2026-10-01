package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExamenModelTest {
    private final ExamenService servicio = mock(ExamenService.class);
    private ExamenModel modelo() throws Exception {
        ExamenModel model = new ExamenModel();
        var campo = ExamenModel.class.getDeclaredField("examenService"); campo.setAccessible(true); campo.set(model, servicio);
        return model;
    }
    @Test void cancelarEdicionNoModificaLaFilaOriginal() throws Exception {
        ExamenModel model = modelo(); Examen original = new Examen(UUID.randomUUID()); original.setNombre("Original");
        UUID tipo = UUID.randomUUID(); when(servicio.tiposAsignados(original.getIdExamen())).thenReturn(List.of(tipo));
        model.editar(original); model.getSeleccionada().setNombre("Modificado"); model.cancelar();
        assertEquals("Original", original.getNombre()); assertNull(model.getSeleccionada()); assertTrue(model.getTiposIds().isEmpty());
    }
    @Test void errorConservaFormularioYTiposParaReintentar() throws Exception {
        ExamenModel model = modelo(); model.nuevo(); model.getSeleccionada().setNombre("Hemograma");
        UUID tipo = UUID.randomUUID(); model.setTiposIds(new ArrayList<>(List.of(tipo.toString())));
        doThrow(new ServiceException("Tipo no disponible")).when(servicio).guardarConTipos(any(), anyList());
        FacesContext faces = mock(FacesContext.class);
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class); MockedStatic<Mensajes> mensajes = mockStatic(Mensajes.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            model.guardar();
            assertNotNull(model.getSeleccionada()); assertNull(model.getSeleccionada().getIdExamen());
            assertEquals(List.of(tipo.toString()), model.getTiposIds()); verify(faces).validationFailed();
            verify(servicio, never()).listarTodos();
        }
    }
    @Test void guardadoLimpiaFormularioYRecargaCatalogo() throws Exception {
        ExamenModel model = modelo(); model.nuevo(); assertTrue(model.getSeleccionada().getActivo());
        model.getSeleccionada().setNombre("Hemograma"); UUID tipo = UUID.randomUUID(); model.setTiposIds(List.of(tipo.toString()));
        Examen formulario = model.getSeleccionada();
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class); MockedStatic<Mensajes> mensajes = mockStatic(Mensajes.class)) { model.guardar(); }
        verify(servicio).guardarConTipos(formulario, List.of(tipo)); verify(servicio).listarTodos();
        assertNull(model.getSeleccionada()); assertTrue(model.getTiposIds().isEmpty());
    }
}
