package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.application.Application;
import jakarta.faces.context.FacesContext;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroPersonaModelTest {
    @Mock PersonaModel personaModel;
    @Mock PersonaService personaService;
    @Mock DocumentoService documentoService;
    @Mock MedioContactoService medioContactoService;
    @Mock TipoDocumentoService tipoDocumentoService;
    @Mock TipoMedioContactoService tipoMedioContactoService;
    @Mock FacesContext context;
    @Mock Application application;
    @InjectMocks RegistroPersonaModel model;

    private abstract static class Contexto extends FacesContext {
        static void establecer(FacesContext contexto) { setCurrentInstance(contexto); }
    }

    @BeforeEach
    void contexto() {
        Contexto.establecer(context);
        lenient().when(context.getApplication()).thenReturn(application);
        lenient().when(application.getResourceBundle(context, "msg"))
                .thenReturn(ResourceBundle.getBundle("i18n.Messages", Locale.forLanguageTag("es")));
    }

    @AfterEach
    void limpiar() { Contexto.establecer(null); }

    @Test
    void guardarPersonaMantieneSeleccionYHabilitaRelaciones() {
        Persona persona = new Persona();
        persona.setNombres(" Ana ");
        persona.setApellidos(" López ");
        when(personaModel.getSeleccionada()).thenReturn(persona);
        doAnswer(invocation -> { persona.setIdPersona(UUID.randomUUID()); return null; })
                .when(personaService).crear(persona);
        model.guardarPersona();
        assertTrue(model.isPersonaGuardada());
        assertEquals("Ana", persona.getNombres());
        verify(personaModel).cargarPersonas();
        verify(personaModel, never()).cancelar();
        verify(context, never()).validationFailed();
    }

    @Test
    void insertFallidoNoDejaUuidNiHabilitaRelaciones() {
        Persona persona = new Persona();
        persona.setNombres("Ana");
        persona.setApellidos("López");
        when(personaModel.getSeleccionada()).thenReturn(persona);
        doAnswer(invocation -> {
            persona.setIdPersona(UUID.randomUUID());
            throw new ServiceException("Error");
        }).when(personaService).crear(persona);
        model.guardarPersona();
        assertNull(persona.getIdPersona());
        assertFalse(model.isPersonaGuardada());
        verify(context).validationFailed();
    }

    @Test
    void noGuardaDocumentoAntesDeGuardarPersona() {
        model.guardarDocumento();
        verifyNoInteractions(documentoService, tipoDocumentoService);
        verify(context).validationFailed();
    }

    private Persona seleccionarPersona() {
        Persona persona = new Persona(UUID.randomUUID());
        model.editar(persona);
        when(personaService.buscarPorId(persona.getIdPersona())).thenReturn(persona);
        return persona;
    }

    private TipoDocumento tipoDocumento(String patron) {
        TipoDocumento tipo = new TipoDocumento(UUID.randomUUID());
        tipo.setActivo(true);
        tipo.setExpresionRegular(patron);
        model.getDocumento().setIdTipoDocumento(tipo);
        when(tipoDocumentoService.buscarPorId(tipo.getIdTipoDocumento())).thenReturn(tipo);
        return tipo;
    }

    @Test
    void documentoGuardaLasRelacionesYLaRutaYPreparaOtroRegistro() {
        Persona persona = seleccionarPersona();
        TipoDocumento tipo = tipoDocumento("[0-9]{8}-[0-9]");
        Documento borrador = model.getDocumento();
        borrador.setValor("12345678-9");
        borrador.setRutaFisica("/documentos/dui.pdf");
        model.guardarDocumento();
        verify(documentoService).crear(borrador);
        assertSame(persona, borrador.getIdPersona());
        assertSame(tipo, borrador.getIdTipoDocumento());
        assertEquals("/documentos/dui.pdf", borrador.getRutaFisica());
        assertNotSame(borrador, model.getDocumento());
        verify(context, never()).validationFailed();
    }

    @Test
    void rechazaValorQueNoCumpleElPatron() {
        seleccionarPersona();
        tipoDocumento("[0-9]{8}-[0-9]");
        model.getDocumento().setValor("invalido");
        model.guardarDocumento();
        verify(documentoService, never()).crear(any());
        assertEquals("invalido", model.getDocumento().getValor());
        verify(context).validationFailed();
    }

    @Test
    void rechazaPatronInvalidoDelCatalogoSinLanzarExcepcion() {
        seleccionarPersona();
        tipoDocumento("[");
        model.getDocumento().setValor("1234");
        assertDoesNotThrow(model::guardarDocumento);
        verify(documentoService, never()).crear(any());
        verify(context).validationFailed();
    }

    @Test
    void revalidaTipoInactivadoDespuesDeSeleccionarlo() {
        seleccionarPersona();
        TipoDocumento tipo = tipoDocumento(".");
        tipo.setActivo(false);
        model.getDocumento().setValor("1234");
        model.guardarDocumento();
        verify(documentoService, never()).crear(any());
        verify(context).validationFailed();
    }

    @Test
    void guardaContactoConPersonaYTipoPersistidos() {
        Persona persona = seleccionarPersona();
        TipoMedioContacto tipo = new TipoMedioContacto(UUID.randomUUID());
        tipo.setActivo(true);
        tipo.setExpresionRegular("[0-9]{8}");
        when(tipoMedioContactoService.buscarPorId(tipo.getIdTipoMedioContacto())).thenReturn(tipo);
        MedioContacto borrador = model.getContacto();
        borrador.setIdTipoMedioContacto(tipo);
        borrador.setValor("76543210");
        model.guardarContacto();
        verify(medioContactoService).crear(borrador);
        assertSame(persona, borrador.getIdPersona());
        assertSame(tipo, borrador.getIdTipoMedioContacto());
        assertNotSame(borrador, model.getContacto());
    }

    @Test
    void autocompleteFiltraActivosYBuscaSinDistinguirMayusculas() {
        TipoDocumento activo = new TipoDocumento(UUID.randomUUID());
        activo.setNombre("Pasaporte");
        activo.setActivo(true);
        TipoDocumento inactivo = new TipoDocumento(UUID.randomUUID());
        inactivo.setNombre("Pasaporte anterior");
        inactivo.setActivo(false);
        when(tipoDocumentoService.listarTodos()).thenReturn(List.of(activo, inactivo));
        assertEquals(List.of(activo), model.completarTiposDocumento("PASA"));
        assertEquals(List.of(activo), model.completarTiposDocumento(""));
    }

    @Test
    void cambiarPersonaLimpiaBorradoresYListas() {
        Persona primera = new Persona(UUID.randomUUID());
        Documento documento = new Documento(UUID.randomUUID());
        when(documentoService.listarPorPersona(primera.getIdPersona())).thenReturn(List.of(documento));
        model.editar(primera);
        model.getContacto().setValor("pendiente");
        model.nuevo();
        assertFalse(model.isPersonaGuardada());
        assertTrue(model.getDocumentos().isEmpty());
        assertNull(model.getContacto().getValor());
        assertEquals(0, model.getPestana());
    }
}
