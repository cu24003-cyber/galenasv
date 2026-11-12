package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.IntentosAcceso.Ambito;

class IntentosAccesoTest {
    private final Instant ahora = Instant.parse("2026-11-06T12:00:00Z");

    @Test void limitaIntentosPorDireccionYLiberaLaVentanaExpirada() {
        IntentosAcceso limite = new IntentosAcceso();
        for (int i = 0; i < 10; i++) limite.registrar("cliente-a", ahora);
        assertEquals(429, assertThrows(AccesoException.class, () -> limite.registrar("cliente-a", ahora)).getStatus());
        assertDoesNotThrow(() -> limite.registrar("cliente-b", ahora));
        assertDoesNotThrow(() -> limite.registrar("cliente-a", ahora.plusSeconds(300)));
    }

    @Test void registrarSinAmbitoCuentaComoLogin() {
        IntentosAcceso limite = new IntentosAcceso();
        for (int i = 0; i < 10; i++) limite.registrar("cliente-a", ahora);
        assertThrows(AccesoException.class, () -> limite.registrar(Ambito.LOGIN, "cliente-a", ahora));
    }

    @Test void busquedaPublicaPermiteSesentaPorVentanaConSuPropioCodigo() {
        IntentosAcceso limite = new IntentosAcceso();
        for (int i = 0; i < 60; i++) limite.registrar(Ambito.BUSQUEDA_PUBLICA, "cliente-a", ahora);
        AccesoException error = assertThrows(AccesoException.class, () -> limite.registrar(Ambito.BUSQUEDA_PUBLICA, "cliente-a", ahora));
        assertEquals(429, error.getStatus()); assertEquals("publico.demasiadasSolicitudes", error.getMessageKey());
        assertDoesNotThrow(() -> limite.registrar(Ambito.BUSQUEDA_PUBLICA, "cliente-a", ahora.plusSeconds(300)));
    }

    @Test void losAmbitosTienenContadoresIndependientes() {
        IntentosAcceso limite = new IntentosAcceso();
        for (int i = 0; i < 60; i++) limite.registrar(Ambito.BUSQUEDA_PUBLICA, "cliente-a", ahora);
        assertDoesNotThrow(() -> limite.registrar(Ambito.LOGIN, "cliente-a", ahora), "buscar no debe bloquear el login");
        for (int i = 0; i < 10; i++) limite.registrar(Ambito.LOGIN, "cliente-b", ahora);
        assertThrows(AccesoException.class, () -> limite.registrar(Ambito.LOGIN, "cliente-b", ahora));
        assertDoesNotThrow(() -> limite.registrar(Ambito.BUSQUEDA_PUBLICA, "cliente-b", ahora), "el login agotado no bloquea la búsqueda");
    }
}
