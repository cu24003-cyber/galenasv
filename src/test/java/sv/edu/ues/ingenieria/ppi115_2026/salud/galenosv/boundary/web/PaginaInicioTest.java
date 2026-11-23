package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** La raíz /galeno/ sirve index.html, que debe llevar a la pantalla de clínicas sin JavaScript. */
class PaginaInicioTest {
    private static String inicio() throws Exception {
        return Files.readString(Path.of("src/main/webapp/index.html"), StandardCharsets.UTF_8);
    }

    @Test void redirigeConMetaRefreshRelativoAClinica() throws Exception {
        String html = inicio();
        assertTrue(html.matches("(?s).*<meta\\s+http-equiv=\"refresh\"\\s+content=\"0;\\s*url=paginas/clinica\\.xhtml\"\\s*/?>.*"), html);
    }

    @Test void muestraEnlaceVisibleEnEspanolYNoUsaJavaScript() throws Exception {
        String html = inicio();
        assertTrue(html.contains("<html lang=\"es\">"));
        assertTrue(html.matches("(?s).*<a href=\"paginas/clinica\\.xhtml\">[^<]+</a>.*"));
        assertFalse(html.contains("Hello World"));
        assertFalse(html.toLowerCase().contains("<script"));
        assertFalse(html.toLowerCase().contains("javascript:"));
    }
}
