package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

@ExtendWith(MockitoExtension.class)
class PublicoResourceTest {
    @Mock TipoDocumentoService tipos;
    @InjectMocks PublicoResource resource;

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

    @Test void laRutaEsPublicaYNoUsaLaSesion() {
        assertEquals("publico", PublicoResource.class.getAnnotation(jakarta.ws.rs.Path.class).value());
        // El recurso solo depende del catálogo; no inyecta AccesoSesion ni AtencionSesion.
        assertTrue(java.util.Arrays.stream(PublicoResource.class.getDeclaredFields())
                .noneMatch(f -> f.getType().getSimpleName().endsWith("Sesion")));
    }
}
