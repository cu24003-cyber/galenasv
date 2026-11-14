package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.Json;
import jakarta.json.bind.JsonbBuilder;
import java.io.StringReader;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

class AtencionDtoTest {
    private static PersonaRol asignacion(String codigo, String nombre) {
        Rol rol = new Rol(UUID.randomUUID()); rol.setCodigo(codigo); rol.setNombre(nombre); rol.setActivo(true);
        Clinica clinica = new Clinica(UUID.randomUUID()); clinica.setNombre("Clínica Santa Ana");
        PersonaRol a = new PersonaRol(UUID.randomUUID()); a.setIdRol(rol); a.setIdClinica(clinica);
        return a;
    }

    private static jakarta.json.JsonObject json(AtencionDto.Asignacion dto) throws Exception {
        try (var jsonb = JsonbBuilder.create()) {
            return Json.createReader(new StringReader(jsonb.toJson(dto))).readObject();
        }
    }

    @Test void unaAsignacionDeEnfermeraLlevaSuCodigoYNombre() throws Exception {
        PersonaRol a = asignacion("ENFERMERA", "Enfermera");
        var json = json(AtencionDto.Asignacion.de(a, a.getIdPersonaRol()));
        assertEquals("ENFERMERA", json.getString("codigoRol"));
        assertEquals("Enfermera", json.getString("nombreRol"));
        assertEquals("Clínica Santa Ana", json.getString("nombreClinica"));
        assertTrue(json.getBoolean("activa"));
    }

    @Test void unRolSinCodigoSaleConCodigoRolNullYNoSeOmite() throws Exception {
        var json = json(AtencionDto.Asignacion.de(asignacion(null, "Laboratorista"), UUID.randomUUID()));
        assertTrue(json.containsKey("codigoRol"), "el frontend espera la clave aunque sea null");
        assertTrue(json.isNull("codigoRol"));
        assertFalse(json.getBoolean("activa"));
    }
}
