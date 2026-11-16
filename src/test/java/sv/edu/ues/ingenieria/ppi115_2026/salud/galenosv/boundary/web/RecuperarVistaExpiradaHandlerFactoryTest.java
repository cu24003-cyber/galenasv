package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.web;

import jakarta.faces.application.ViewExpiredException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecuperarVistaExpiradaHandlerFactoryTest {
    @Test void reconoceLaVistaExpiradaAunqueVengaEnvuelta() {
        assertTrue(RecuperarVistaExpiradaHandlerFactory.contieneVistaExpirada(
                new RuntimeException("Error al restaurar", new ViewExpiredException("/paginas/consultas.jsf"))));
        assertFalse(RecuperarVistaExpiradaHandlerFactory.contieneVistaExpirada(new RuntimeException("Otro error")));
    }
}
