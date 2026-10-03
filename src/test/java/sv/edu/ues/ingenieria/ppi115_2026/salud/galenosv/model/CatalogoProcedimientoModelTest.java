package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Procedimiento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoProcedimientoModelTest {
    @Mock ConfiguracionProcedimientoService servicio;
    @InjectMocks CatalogoProcedimientoModel model;

    @Test void iniciaOcultoYElBotonAbreUnProcedimientoNuevo() {
        model.iniciar(); assertNull(model.getSeleccionado());
        model.nuevo(); assertNotNull(model.getSeleccionado()); assertTrue(model.getSeleccionado().getActivo());
        try (var estado = mockStatic(FormularioCancelacion.class)) {
            estado.when(FormularioCancelacion::estaVacio).thenReturn(false, true);
            model.getSeleccionado().setNombre("Nuevo"); model.cancelar();
            assertNotNull(model.getSeleccionado()); assertNull(model.getSeleccionado().getNombre());
            model.cancelar(); assertNull(model.getSeleccionado());
        }
    }

    @Test void eliminarSeleccionadoSinUsoRecargaLaListaYCierraElEditor() {
        Procedimiento procedimiento = new Procedimiento(UUID.randomUUID());
        model.seleccionar(procedimiento); when(servicio.listar()).thenReturn(List.of());
        FacesContext faces = mock(FacesContext.class);
        try (var contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            model.eliminar(procedimiento);
            verify(servicio).eliminar(procedimiento.getIdProcedimiento());
            assertNull(model.getSeleccionado()); assertTrue(model.getLista().isEmpty());
        }
    }

    @Test void eliminarUsadoConservaLaListaYElProcedimiento() {
        Procedimiento procedimiento = new Procedimiento(UUID.randomUUID());
        model.seleccionar(procedimiento);
        doThrow(new ServiceException("procedimiento.eliminarEnUso", "Ya registrado", null))
                .when(servicio).eliminar(procedimiento.getIdProcedimiento());
        FacesContext faces = mock(FacesContext.class);
        try (var contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            model.eliminar(procedimiento); assertNotNull(model.getSeleccionado());
            verify(faces).validationFailed(); verify(servicio, never()).listar();
        }
    }
}
