package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.CitaPublicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.CitaPublicaService.CitaPublica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.CitaPublicaService.ClinicaPublica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.IntentosAcceso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

@ExtendWith(MockitoExtension.class)
class PublicoResourceTest {
    @Mock TipoDocumentoService tipos;
    @Mock CitaPublicaService citas;
    @Mock IntentosAcceso intentos;
    @Mock HttpServletRequest request;
    @InjectMocks PublicoResource resource;
    private final UUID dui = UUID.randomUUID(), clinica = UUID.randomUUID(), idConsulta = UUID.randomUUID();
    private static final List<String> PROHIBIDOS = List.of("observaciones", "valor", "estado", "responsable",
            "idPersonaRol", "referenciaExterna", "resultado", "numeroDocumento", "idPersona");

    @BeforeEach void preparar() {
        resource.validador = Validation.buildDefaultValidatorFactory().getValidator();
        lenient().when(request.getRemoteAddr()).thenReturn("192.0.2.20");
    }
    private PublicoResource.BusquedaDocumento documento(UUID tipo, String numero) {
        var b = new PublicoResource.BusquedaDocumento(); b.idTipoDocumento = tipo; b.numeroDocumento = numero; return b;
    }
    private PublicoResource.BusquedaCitas busqueda(String numero) {
        var b = new PublicoResource.BusquedaCitas(); b.idTipoDocumento = dui; b.numeroDocumento = numero;
        b.idClinica = clinica; b.desde = LocalDate.of(2026, 11, 1); b.hasta = LocalDate.of(2026, 11, 30); return b;
    }
    private static String cuerpo(jakarta.ws.rs.core.Response r) { return (String) r.getEntity(); }

    private TipoDocumento tipo(String nombre, Boolean activo) {
        TipoDocumento t = new TipoDocumento(UUID.randomUUID()); t.setNombre(nombre); t.setActivo(activo);
        t.setIndicaciones("Ocho dígitos, guion y dígito verificador."); t.setExpresionRegular("^[0-9]{8}-[0-9]$");
        return t;
    }

    @Test void tiposDocumentoDevuelveSoloActivosConLosCuatroCamposDelLogin() {
        TipoDocumento dui = tipo("DUI", true), inactivo = tipo("Antiguo", false);
        when(tipos.listarActivos()).thenReturn(List.of(dui, inactivo));
        var arreglo = Json.createReader(new StringReader((String) resource.tiposDocumento().getEntity())).readArray();
        assertEquals(1, arreglo.size());
        JsonObject json = arreglo.getJsonObject(0);
        assertEquals(Set.of("idTipoDocumento", "nombre", "indicaciones", "expresionRegular"), json.keySet());
        assertEquals(dui.getIdTipoDocumento().toString(), json.getString("idTipoDocumento"));
        assertEquals("DUI", json.getString("nombre"));
        assertEquals("^[0-9]{8}-[0-9]$", json.getString("expresionRegular"));
    }

    @Test void camposOpcionalesVaciosSeDevuelvenComoNull() {
        TipoDocumento t = tipo("Pasaporte", true); t.setIndicaciones(null); t.setExpresionRegular(null);
        when(tipos.listarActivos()).thenReturn(List.of(t));
        var json = Json.createReader(new StringReader((String) resource.tiposDocumento().getEntity())).readArray().getJsonObject(0);
        assertTrue(json.isNull("indicaciones")); assertTrue(json.isNull("expresionRegular"));
    }

    // --- POST /publico/clinicas ---
    @Test void clinicasDevuelveIdYNombreYCuentaElIntento() {
        when(citas.clinicas(dui, "00000000-3")).thenReturn(List.of(new ClinicaPublica(clinica, "Clínica Uno")));
        var arreglo = Json.createReader(new StringReader(cuerpo(resource.clinicas(documento(dui, "00000000-3"))))).readArray();
        assertEquals(Set.of("idClinica", "nombre"), arreglo.getJsonObject(0).keySet());
        assertEquals(clinica.toString(), arreglo.getJsonObject(0).getString("idClinica"));
        verify(intentos).registrar(IntentosAcceso.Ambito.BUSQUEDA_PUBLICA, "192.0.2.20");
    }
    @Test void clinicasDocumentoInexistenteYSinRegistroDevuelvenLoMismo() {
        when(citas.clinicas(dui, "99999999-9")).thenReturn(List.of());
        when(citas.clinicas(dui, "00000000-1")).thenReturn(List.of());
        var inexistente = resource.clinicas(documento(dui, "99999999-9"));
        var sinRegistro = resource.clinicas(documento(dui, "00000000-1"));
        assertEquals(200, inexistente.getStatus()); assertEquals(sinRegistro.getStatus(), inexistente.getStatus());
        assertEquals(cuerpo(sinRegistro), cuerpo(inexistente)); assertEquals("[]", cuerpo(inexistente));
    }
    @Test void clinicasConDemasiadasSolicitudesNoConsultaLaBase() {
        doThrow(new AccesoException(429, "publico.demasiadasSolicitudes", "Espere.")).when(intentos)
                .registrar(IntentosAcceso.Ambito.BUSQUEDA_PUBLICA, "192.0.2.20");
        assertEquals(429, assertThrows(AccesoException.class, () -> resource.clinicas(documento(dui, "00000000-3"))).getStatus());
        verifyNoInteractions(citas);
    }
    @Test void cuerpoInvalidoEs400PeroTambienCuentaComoIntento() {
        assertThrows(ConstraintViolationException.class, () -> resource.clinicas(documento(null, "00000000-3")));
        assertThrows(ConstraintViolationException.class, () -> resource.clinicas(documento(dui, " ")));
        assertThrows(ConstraintViolationException.class, () -> resource.clinicas(documento(dui, "1".repeat(51))));
        assertThrows(ConstraintViolationException.class, () -> resource.clinicas(documento(dui, "0\r\n1")));
        assertEquals(400, assertThrows(AccesoException.class, () -> resource.clinicas(null)).getStatus());
        verify(intentos, times(5)).registrar(IntentosAcceso.Ambito.BUSQUEDA_PUBLICA, "192.0.2.20");
        verifyNoInteractions(citas);
    }

    // --- POST /publico/citas ---
    @Test void citasSoloDevuelveLosCuatroCamposPublicos() {
        var cita = new CitaPublica(idConsulta, OffsetDateTime.parse("2026-11-12T14:00Z"), "Clínica Uno", List.of("Consulta general"));
        when(citas.citas(dui, "00000000-3", clinica, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(List.of(cita));
        String body = cuerpo(resource.citas(busqueda("00000000-3")));
        var json = Json.createReader(new StringReader(body)).readArray().getJsonObject(0);
        assertEquals(Set.of("idConsulta", "fechaInicio", "nombreClinica", "procedimientos"), json.keySet());
        assertEquals("2026-11-12T08:00:00-06:00", json.getString("fechaInicio"), "fecha en hora de El Salvador");
        assertEquals("Consulta general", json.getJsonArray("procedimientos").getString(0));
        for (String campo : PROHIBIDOS) assertFalse(body.contains(campo), campo);
        verify(intentos).registrar(IntentosAcceso.Ambito.BUSQUEDA_PUBLICA, "192.0.2.20");
    }
    @Test void citasDocumentoInexistenteYDocumentoSinCitasDevuelvenLoMismo() {
        when(citas.citas(eq(dui), anyString(), any(), any(), any())).thenReturn(List.of());
        var inexistente = resource.citas(busqueda("99999999-9"));
        var sinCitas = resource.citas(busqueda("00000000-3"));
        assertEquals(sinCitas.getStatus(), inexistente.getStatus()); assertEquals(cuerpo(sinCitas), cuerpo(inexistente));
    }
    @Test void citasExigeClinicaYFechas() {
        var sinClinica = busqueda("00000000-3"); sinClinica.idClinica = null;
        var sinFecha = busqueda("00000000-3"); sinFecha.hasta = null;
        assertThrows(ConstraintViolationException.class, () -> resource.citas(sinClinica));
        assertThrows(ConstraintViolationException.class, () -> resource.citas(sinFecha));
        verifyNoInteractions(citas);
    }

    @Test void laRutaEsPublicaYNoUsaLaSesion() {
        assertEquals("publico", PublicoResource.class.getAnnotation(jakarta.ws.rs.Path.class).value());
        // El recurso solo depende del catálogo; no inyecta AccesoSesion ni AtencionSesion.
        assertTrue(java.util.Arrays.stream(PublicoResource.class.getDeclaredFields())
                .noneMatch(f -> f.getType().getSimpleName().endsWith("Sesion")));
    }
}
