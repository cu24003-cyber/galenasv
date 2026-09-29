package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import java.util.UUID;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.Test;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoMedioContactoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TipoMedioContactoModelTest {
    @Test
    void cancelarNoModificaElRegistroOriginal() {
        TipoMedioContactoModel model = new TipoMedioContactoModel();
        TipoMedioContacto original = new TipoMedioContacto(UUID.randomUUID());
        original.setNombre("Original");
        original.setIndicaciones("Indicaciones");
        original.setExpresionRegular("[0-9]+");
        original.setActivo(true);
        model.editar(original);
        assertEquals(original.getExpresionRegular(), model.getSeleccionada().getExpresionRegular());
        model.getSeleccionada().setNombre("Modificado");
        model.cancelar();
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
}
