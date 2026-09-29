package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.application.Application;
import jakarta.faces.context.FacesContext;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ClinicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicaModelTest {
    @Mock ClinicaService service;
    @Mock FacesContext context;
    @Mock Application application;
    @InjectMocks ClinicaModel model;

    private abstract static class Contexto extends FacesContext {
        static void establecer(FacesContext context) { setCurrentInstance(context); }
    }

    private void prepararMensajes() {
        Contexto.establecer(context);
        when(context.getApplication()).thenReturn(application);
        when(application.getResourceBundle(context, "msg"))
                .thenReturn(ResourceBundle.getBundle("i18n.Messages", Locale.forLanguageTag("es")));
    }

    @AfterEach
    void limpiar() { Contexto.establecer(null); }

    @Test
    void guardarOcultaFormularioYConservaFiltro() {
        prepararMensajes();
        model.setSearchName("Central");
        model.nuevo();
        Clinica nueva = model.getSeleccionada();
        nueva.setNombre("Central");
        when(service.searchByName("Central")).thenReturn(List.of(nueva));
        model.guardar();
        verify(service).crear(nueva);
        assertNull(model.getSeleccionada());
        assertEquals(List.of(nueva), model.getClinicas());
        assertEquals("Central", model.getSearchName());
    }

    @Test
    void errorAlCrearConservaFormularioYPermiteReintentarCreacion() {
        prepararMensajes();
        model.nuevo();
        Clinica nueva = model.getSeleccionada();
        nueva.setNombre("Central");
        doAnswer(invocation -> {
            nueva.setIdClinica(UUID.randomUUID());
            throw new ServiceException("Error de persistencia");
        }).when(service).crear(nueva);
        model.guardar();
        assertSame(nueva, model.getSeleccionada());
        assertNull(nueva.getIdClinica());
        assertEquals("Central", nueva.getNombre());
        verify(context).validationFailed();
        verify(service, never()).listarTodos();
    }

    @Test
    void cancelarEdicionNoModificaLaFilaOriginal() {
        Clinica original = new Clinica(UUID.randomUUID(), "Central");
        model.editar(original);
        model.getSeleccionada().setNombre("Modificada");
        model.cancelar();
        assertNull(model.getSeleccionada());
        assertEquals("Central", original.getNombre());
        verifyNoInteractions(service);
    }

    @Test
    void guardarEdicionActualizaClinica() {
        prepararMensajes();
        Clinica original = new Clinica(UUID.randomUUID(), "Central");
        model.editar(original);
        Clinica editada = model.getSeleccionada();
        editada.setNombre("Nueva sede");
        model.guardar();
        verify(service).actualizar(editada);
        verify(service, never()).crear(any());
        assertNull(model.getSeleccionada());
    }

    @Test
    void filtroVacioCargaUnaSolaVezYReiniciaPaginacion() {
        when(service.listarTodos()).thenReturn(List.of());
        model.setSearchName(" ");
        model.setPrimeraFila(20);
        model.cargarClinicas();
        verify(service).listarTodos();
        verify(service, never()).searchByName(any());
        assertEquals(0, model.getPrimeraFila());
    }
}
