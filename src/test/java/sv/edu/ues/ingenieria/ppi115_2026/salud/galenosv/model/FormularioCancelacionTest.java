package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FormularioCancelacionTest {
    @Test void cancelarLimpiaDatosNoEnviadosYUnSegundoClickOcultaLosEditores() throws Exception {
        FacesContext faces = mock(FacesContext.class);
        ExternalContext externo = mock(ExternalContext.class);
        var parametros = new HashMap<String, String>();
        when(faces.getExternalContext()).thenReturn(externo);
        when(externo.getRequestParameterMap()).thenReturn(parametros);
        try (var contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            for (AbstractModel<?, ?> model : List.of(new PersonaModel(), new ClinicaModel(), new TipoDocumentoModel(),
                    new TipoMedioContactoModel(), new TipoExamenModel(), new ExamenModel())) {
                model.nuevo();
                var anterior = model.getSeleccionada();
                parametros.put("galeno.formularioVacio", "false");
                model.getClass().getMethod("cancelar").invoke(model);
                assertNotNull(model.getSeleccionada());
                assertNotSame(anterior, model.getSeleccionada());
                parametros.put("galeno.formularioVacio", "true");
                model.getClass().getMethod("cancelar").invoke(model);
                assertNull(model.getSeleccionada());
            }
        }
    }

    @Test void documentosYContactosPuedenOcultarseYReabrirsePorSeparado() {
        RegistroPersonaModel model = new RegistroPersonaModel();
        FacesContext faces = mock(FacesContext.class);
        ExternalContext externo = mock(ExternalContext.class);
        var parametros = new HashMap<String, String>();
        when(faces.getExternalContext()).thenReturn(externo);
        when(externo.getRequestParameterMap()).thenReturn(parametros);
        try (var contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            parametros.put("galeno.formularioVacio", "false");
            model.getDocumento().setValor("12345678-9"); model.cancelarDocumento();
            assertNull(model.getDocumento().getValor()); assertTrue(model.isDocumentoVisible());
            model.getContacto().setValor("7777-7777"); model.cancelarContacto();
            assertNull(model.getContacto().getValor()); assertTrue(model.isContactoVisible());
            parametros.put("galeno.formularioVacio", "true");
            model.cancelarDocumento(); assertFalse(model.isDocumentoVisible()); assertTrue(model.isContactoVisible());
            model.cancelarContacto(); assertFalse(model.isContactoVisible());
            model.nuevoDocumento(); model.nuevoContacto();
            assertTrue(model.isDocumentoVisible()); assertTrue(model.isContactoVisible());
        }
    }

    @Test void rolConDatosSeLimpiaYVuelveAlModoDeCreacion() {
        RolModel model = new RolModel();
        model.newRol(); model.getRolSeleccionado().setNombre("Doctor");
        try (var estado = mockStatic(FormularioCancelacion.class)) {
            estado.when(FormularioCancelacion::estaVacio).thenReturn(false, true);
            model.cancelRol(); assertNotNull(model.getRolSeleccionado());
            assertNull(model.getRolSeleccionado().getNombre()); assertTrue(model.getRolSeleccionado().getActivo());
            model.cancelRol(); assertNull(model.getRolSeleccionado());
        }
    }
}
