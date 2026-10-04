package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.faces.component.*;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FormularioCancelacionTest {
    @Test void leeElValorEnviadoInclusoSiEsInvalidoOSiElModeloTodaviaTieneDatos() {
        FacesContext faces = mock(FacesContext.class);
        ExternalContext externo = mock(ExternalContext.class);
        UIViewRoot vista = mock(UIViewRoot.class);
        UIForm formulario = mock(UIForm.class);
        UIComponent boton = mock(UIComponent.class);
        UIInput campo = mock(UIInput.class);
        var valor = new AtomicReference<Object>("Ana1@_");
        when(faces.getExternalContext()).thenReturn(externo); when(faces.getViewRoot()).thenReturn(vista);
        when(externo.getRequestParameterMap()).thenReturn(Map.of("jakarta.faces.source", "formulario:cancelar"));
        when(vista.findComponent(":formulario:cancelar")).thenReturn(boton); when(boton.getParent()).thenReturn(formulario);
        when(boton.getAttributes()).thenReturn(Map.of()); when(formulario.getAttributes()).thenReturn(Map.of());
        when(formulario.isRendered()).thenReturn(true); when(campo.isRendered()).thenReturn(true);
        when(campo.getAttributes()).thenReturn(Map.of()); when(campo.getClientId(faces)).thenReturn("formulario:nombre");
        when(campo.getSubmittedValue()).thenAnswer(i -> valor.get());
        when(formulario.getFacetsAndChildren()).thenAnswer(i -> List.<UIComponent>of(campo).iterator());
        when(campo.getFacetsAndChildren()).thenAnswer(i -> List.<UIComponent>of().iterator());
        try (var contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            assertFalse(AbstractModel.formularioVacio());
            valor.set(""); assertTrue(AbstractModel.formularioVacio());
            verify(campo, never()).getValue(); // Un campo borrado no usa el antiguo valor del Model.
        }
    }

    @Test void cancelarLimpiaLuegoOcultaLosEditores() throws Exception {
        try (var estado = mockStatic(AbstractModel.class)) {
            for (AbstractModel<?, ?> model : List.of(new PersonaModel(), new ClinicaModel(), new TipoDocumentoModel(),
                    new TipoMedioContactoModel(), new TipoExamenModel(), new ExamenModel())) {
                model.nuevo(); var anterior = model.getSeleccionada();
                estado.when(AbstractModel::formularioVacio).thenReturn(false, true);
                model.getClass().getMethod("cancelar").invoke(model);
                assertNotNull(model.getSeleccionada()); assertNotSame(anterior, model.getSeleccionada());
                model.getClass().getMethod("cancelar").invoke(model); assertNull(model.getSeleccionada());
            }
        }
    }

    @Test void documentosYContactosPuedenOcultarseYReabrirsePorSeparado() {
        RegistroPersonaModel model = new RegistroPersonaModel();
        try (var estado = mockStatic(AbstractModel.class)) {
            estado.when(AbstractModel::formularioVacio).thenReturn(false);
            model.getDocumento().setValor("12345678-9"); model.cancelarDocumento();
            assertNull(model.getDocumento().getValor()); assertTrue(model.isDocumentoVisible());
            model.getContacto().setValor("7777-7777"); model.cancelarContacto();
            assertNull(model.getContacto().getValor()); assertTrue(model.isContactoVisible());
            estado.when(AbstractModel::formularioVacio).thenReturn(true);
            model.cancelarDocumento(); assertFalse(model.isDocumentoVisible()); assertTrue(model.isContactoVisible());
            model.cancelarContacto(); assertFalse(model.isContactoVisible());
            model.nuevoDocumento(); model.nuevoContacto();
            assertTrue(model.isDocumentoVisible()); assertTrue(model.isContactoVisible());
        }
    }

    @Test void rolConDatosSeLimpiaYVuelveAlModoDeCreacion() {
        RolModel model = new RolModel(); model.newRol(); model.getRolSeleccionado().setNombre("Doctor");
        try (var estado = mockStatic(AbstractModel.class)) {
            estado.when(AbstractModel::formularioVacio).thenReturn(false, true);
            model.cancelRol(); assertNotNull(model.getRolSeleccionado());
            assertNull(model.getRolSeleccionado().getNombre()); assertTrue(model.getRolSeleccionado().getActivo());
            model.cancelRol(); assertNull(model.getRolSeleccionado());
        }
    }
}
