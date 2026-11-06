package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContrasenaServiceTest {
    @Test void generaArgon2ConSalDistintaYConservaLosEspaciosDeLaClave() {
        String password = " contraseña de prueba ";
        String primero = ContrasenaService.generar(password), segundo = ContrasenaService.generar(password);
        ContrasenaService servicio = new ContrasenaService();
        assertTrue(primero.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"));
        assertNotEquals(primero, segundo); assertTrue(servicio.verificar(password, primero));
        assertFalse(servicio.verificar(password.strip(), primero));
        assertFalse(servicio.verificar("otra contraseña", primero));
    }
    @Test void rechazaHashesCorruptosCostesDesmesuradosYOtrosAlgoritmos() {
        var servicio = new ContrasenaService();
        for (String hash : new String[]{"texto", "$argon2id$invalido", "$argon2id$v=19$m=999999999,t=2,p=1$c2FsdA$aGFzaA", "$2a$ejemplo"}) {
            assertFalse(servicio.verificar("contraseña de prueba", hash));
        }
        assertFalse(servicio.verificar("prueba", null));
        assertThrows(IllegalArgumentException.class, () -> ContrasenaService.generar("corta"));
    }
}
