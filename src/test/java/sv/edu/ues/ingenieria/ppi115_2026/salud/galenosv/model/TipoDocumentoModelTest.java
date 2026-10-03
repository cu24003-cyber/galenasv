package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import java.util.UUID;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.Test;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TipoDocumentoModelTest {
    @Test
    void cancelarNoModificaElRegistroOriginal() throws Exception {
        TipoDocumentoModel model = new TipoDocumentoModel();
        TipoDocumentoService service = mock(TipoDocumentoService.class);
        var field = TipoDocumentoModel.class.getDeclaredField("tipoDocumentoService");
        field.setAccessible(true);
        field.set(model, service);
        TipoDocumento original = new TipoDocumento(UUID.randomUUID());
        original.setNombre("Original");
        original.setIndicaciones("Indicaciones");
        original.setExpresionRegular("[0-9]+");
        original.setActivo(true);
        model.editar(original);
        assertEquals(original.getExpresionRegular(), model.getSeleccionada().getExpresionRegular());
        model.getSeleccionada().setNombre("Modificado");
        try (var contexto = mockStatic(FacesContext.class)) { model.cancelar(); }
        assertNull(model.getSeleccionada());
        assertEquals("Original", original.getNombre());
    }

    @Test
    void creacionFallidaPermiteReintentarComoNuevo() throws Exception {
        TipoDocumentoModel model = new TipoDocumentoModel();
        TipoDocumentoService service = mock(TipoDocumentoService.class);
        var field = TipoDocumentoModel.class.getDeclaredField("tipoDocumentoService");
        field.setAccessible(true);
        field.set(model, service);
        model.nuevo();
        TipoDocumento registro = model.getSeleccionada();
        assertTrue(registro.getActivo());
        doAnswer(call -> {
            registro.setIdTipoDocumento(UUID.randomUUID());
            throw new ServiceException("error.general");
        }).doNothing().when(service).crear(registro);
        try (var messages = mockStatic(Mensajes.class);
             var faces = mockStatic(FacesContext.class)) {
            model.guardar();
            assertNull(registro.getIdTipoDocumento());
            assertSame(registro, model.getSeleccionada());
            model.guardar();
        }
        verify(service, times(2)).crear(registro);
        verify(service, never()).actualizar(any());
        assertNull(model.getSeleccionada());
    }

    @Test
    void noAbreEdicionSiElTipoEstaEnUso() throws Exception {
        TipoDocumentoModel model = new TipoDocumentoModel();
        TipoDocumentoService service = mock(TipoDocumentoService.class);
        var field = TipoDocumentoModel.class.getDeclaredField("tipoDocumentoService");
        field.setAccessible(true);
        field.set(model, service);
        TipoDocumento original = new TipoDocumento(UUID.randomUUID());
        when(service.estaEnUso(original.getIdTipoDocumento())).thenReturn(true);
        model.nuevo();
        try (var messages = mockStatic(Mensajes.class);
             var faces = mockStatic(FacesContext.class)) {
            model.editar(original);
            assertNull(model.getSeleccionada());
            messages.verify(() -> Mensajes.texto("tipoDocumento.enUso"));
        }
    }
}
