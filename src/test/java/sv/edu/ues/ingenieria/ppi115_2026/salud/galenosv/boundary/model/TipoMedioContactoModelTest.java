package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.faces.context.FacesContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.Mensajes;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoMedioContactoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoMedioContacto;

class TipoMedioContactoModelTest {
    @Test
    void cancelarNoModificaElRegistroOriginal() throws Exception {
        TipoMedioContactoModel model = new TipoMedioContactoModel();
        TipoMedioContactoService service = mock(TipoMedioContactoService.class);
        var field = TipoMedioContactoModel.class.getDeclaredField("tipoMedioContactoService");
        field.setAccessible(true);
        field.set(model, service);
        TipoMedioContacto original = new TipoMedioContacto(UUID.randomUUID());
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
        TipoMedioContactoModel model = new TipoMedioContactoModel();
        TipoMedioContactoService service = mock(TipoMedioContactoService.class);
        var field = TipoMedioContactoModel.class.getDeclaredField("tipoMedioContactoService");
        field.setAccessible(true);
        field.set(model, service);
        model.nuevo();
        TipoMedioContacto registro = model.getSeleccionada();
        assertTrue(registro.getActivo());
        doAnswer(call -> {
            registro.setIdTipoMedioContacto(UUID.randomUUID());
            throw new ServiceException("error.general");
        }).doNothing().when(service).crear(registro);
        try (var messages = mockStatic(Mensajes.class);
             var faces = mockStatic(FacesContext.class)) {
            model.guardar();
            assertNull(registro.getIdTipoMedioContacto());
            assertSame(registro, model.getSeleccionada());
            model.guardar();
        }
        verify(service, times(2)).crear(registro);
        verify(service, never()).actualizar(any());
        assertNull(model.getSeleccionada());
    }

    @Test
    void noAbreEdicionSiElTipoEstaEnUso() throws Exception {
        TipoMedioContactoModel model = new TipoMedioContactoModel();
        TipoMedioContactoService service = mock(TipoMedioContactoService.class);
        var field = TipoMedioContactoModel.class.getDeclaredField("tipoMedioContactoService");
        field.setAccessible(true);
        field.set(model, service);
        TipoMedioContacto original = new TipoMedioContacto(UUID.randomUUID());
        when(service.estaEnUso(original.getIdTipoMedioContacto())).thenReturn(true);
        model.nuevo();
        try (var messages = mockStatic(Mensajes.class);
             var faces = mockStatic(FacesContext.class)) {
            model.editar(original);
            assertNull(model.getSeleccionada());
            messages.verify(() -> Mensajes.texto("tipoMedioContacto.enUso"));
        }
    }
}
