package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntentosAccesoTest {
    @Test void limitaIntentosPorDireccionYLiberaLaVentanaExpirada() {
        IntentosAcceso limite = new IntentosAcceso(); Instant ahora = Instant.parse("2026-11-06T12:00:00Z");
        for (int i = 0; i < 10; i++) limite.registrar("cliente-a", ahora);
        assertEquals(429, assertThrows(AccesoException.class, () -> limite.registrar("cliente-a", ahora)).getStatus());
        assertDoesNotThrow(() -> limite.registrar("cliente-b", ahora));
        assertDoesNotThrow(() -> limite.registrar("cliente-a", ahora.plusSeconds(300)));
    }
}
