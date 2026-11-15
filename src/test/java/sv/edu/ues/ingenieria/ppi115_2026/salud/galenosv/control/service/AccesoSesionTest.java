package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccesoSesionTest {
    @Test void cadaInicioTieneTokenDistintoYLaSesionExpiraEnOchoHoras() throws Exception {
        AccesoSesion primera = new AccesoSesion(), segunda = new AccesoSesion();
        assertFalse(primera.vigente()); UUID persona = UUID.randomUUID();
        primera.autenticar(persona); segunda.autenticar(persona);
        assertTrue(primera.vigente()); assertEquals(persona, primera.getIdPersona());
        assertNotEquals(primera.getCsrfToken(), segunda.getCsrfToken());
        var inicio = AccesoSesion.class.getDeclaredField("inicio"); inicio.setAccessible(true);
        inicio.set(primera, Instant.now().minusSeconds(8 * 3600 + 1));
        assertFalse(primera.vigente()); assertTrue(segunda.vigente());
    }
}
