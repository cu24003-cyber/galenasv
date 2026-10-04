package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

@ExtendWith(MockitoExtension.class)
class RegistroPersonaModelTest {
    @Mock PersonaModel personaModel;
    @Mock PersonaService personaService;
    @Mock RegistroPersonaService registroService;
    @Mock RolService rolService;
    @Mock ClinicaService clinicaService;
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
                .when(registroService).guardar(persona, null, null, null);
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
        }).when(registroService).guardar(persona, null, null, null);
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

    @Test
    void cambiarYLimpiarTiposActualizaMascarasYDescartaValoresAnteriores() {
        TipoDocumento tipo = new TipoDocumento();
        tipo.setExpresionRegular("[0-9]{8}-[0-9]");
        model.getDocumento().setIdTipoDocumento(tipo);
        model.getDocumento().setValor("anterior");
        assertEquals("99999999-9", model.getMascaraDocumento());
        model.cambiarTipoDocumento();
        assertNull(model.getDocumento().getValor());
        model.limpiarTipoDocumento();
        assertNull(model.getDocumento().getIdTipoDocumento());
        assertNull(model.getMascaraDocumento());

        TipoMedioContacto contacto = new TipoMedioContacto();
        contacto.setExpresionRegular("[0-9]{4}-[0-9]{4}");
        model.getContacto().setIdTipoMedioContacto(contacto);
        model.getContacto().setValor("anterior");
        assertEquals("9999-9999", model.getMascaraContacto());
        model.cambiarTipoContacto();
        assertNull(model.getContacto().getValor());
        model.limpiarTipoContacto();
        assertNull(model.getContacto().getIdTipoMedioContacto());
        assertNull(model.getMascaraContacto());
    }

    @Test
    void contactoSeValidaConElPatronActualDeBaseAlGuardar() {
        seleccionarPersona();
        TipoMedioContacto seleccionado = new TipoMedioContacto(UUID.randomUUID());
        seleccionado.setExpresionRegular(".");
        model.getContacto().setIdTipoMedioContacto(seleccionado);
        model.getContacto().setValor("77777777");
        TipoMedioContacto actual = new TipoMedioContacto(seleccionado.getIdTipoMedioContacto());
        actual.setActivo(true);
        actual.setExpresionRegular("[0-9]{4}-[0-9]{4}");
        when(tipoMedioContactoService.buscarPorId(actual.getIdTipoMedioContacto())).thenReturn(actual);
        model.guardarContacto();
        verify(medioContactoService, never()).crear(any());
        verify(context).validationFailed();
        assertEquals("77777777", model.getContacto().getValor());
    }

    @Test
    void muestraLaReglaDelDuiYConservaElBorradorCuandoHayDuplicado() {
        seleccionarPersona(); tipoDocumento(".");
        Documento borrador = model.getDocumento(); borrador.setValor("12345678-9");
        doThrow(ServiceException.localizada("documento.duiUnico", "Una persona no puede tener más de un DUI."))
                .when(documentoService).crear(borrador);
        model.guardarDocumento();
        assertSame(borrador, model.getDocumento()); assertNull(borrador.getIdDocumento());
        var mensaje = org.mockito.ArgumentCaptor.forClass(jakarta.faces.application.FacesMessage.class);
        verify(context).addMessage(isNull(), mensaje.capture());
        assertEquals("Una persona no puede tener más de un DUI.", mensaje.getValue().getSummary());
        verify(context).validationFailed();
    }

    @Test
    void eliminarDocumentoRecargaSoloDocumentosSinGuardarBorradores() {
        Persona persona = new Persona(UUID.randomUUID()); model.editar(persona);
        Documento documento = new Documento(UUID.randomUUID());
        model.getDocumento().setValor("borrador"); model.getContacto().setValor("contacto pendiente");
        when(documentoService.listarPorPersona(persona.getIdPersona())).thenReturn(List.of());
        model.eliminarDocumento(documento);
        verify(documentoService).eliminarDePersona(documento.getIdDocumento(), persona.getIdPersona());
        assertTrue(model.getDocumentos().isEmpty()); assertEquals("borrador", model.getDocumento().getValor());
        assertEquals("contacto pendiente", model.getContacto().getValor());
        verify(documentoService, never()).crear(any()); verify(medioContactoService, never()).crear(any());
    }

    @Test
    void eliminarContactoRecargaContactosSinGuardarElFormulario() {
        Persona persona = new Persona(UUID.randomUUID()); model.editar(persona);
        MedioContacto contacto = new MedioContacto(UUID.randomUUID());
        when(medioContactoService.listarPorPersona(persona.getIdPersona())).thenReturn(List.of());
        model.eliminarContacto(contacto);
        verify(medioContactoService).eliminarDePersona(contacto.getIdMedioContacto(), persona.getIdPersona());
        assertTrue(model.getContactos().isEmpty());
        verify(medioContactoService, never()).crear(any());
    }
}
