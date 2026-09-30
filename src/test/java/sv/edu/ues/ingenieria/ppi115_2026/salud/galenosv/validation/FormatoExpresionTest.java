package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.validation;

import org.junit.jupiter.api.Test;
import java.util.regex.PatternSyntaxException;
import static org.junit.jupiter.api.Assertions.*;

class FormatoExpresionTest {
    @Test
    void mascaraParaDuiTelefonoYDocumentoAlfanumerico() {
        assertEquals("99999999-9", FormatoExpresion.mascara("^[0-9]{8}-[0-9]$"));
        assertEquals("9999-9999", FormatoExpresion.mascara("\\d{4}-\\d{4}"));
        assertEquals("AA-999999", FormatoExpresion.mascara("[A-Z]{2}-[0-9]{6}"));
        assertEquals("aaa***", FormatoExpresion.mascara("[a-zA-Z]{3}[A-Za-z0-9]{3}"));
        assertEquals("99.99", FormatoExpresion.mascara("[0-9]{2}\\.[0-9]{2}"));
    }

    @Test
    void patronesComplejosMantienenEntradaLibreYSeValidanCompletos() {
        String correo = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        assertNull(FormatoExpresion.mascara(correo));
        assertTrue(FormatoExpresion.coincide("ana@example.com", correo));
        assertFalse(FormatoExpresion.coincide("ana@", correo));
        for (String patron : new String[]{"[267][0-9]{7}", "(AB|CD)[0-9]+", "\\d{2,8}", "\\d?", "(?i)[a-z]{3}"}) {
            assertNull(FormatoExpresion.mascara(patron), patron);
        }
    }

    @Test
    void noConfundeLiteralesConTokensDeInputmaskNiEscapes() {
        for (String patron : new String[]{"9[0-9]", "a[0-9]", "#[0-9]", "&[0-9]", "\\s[0-9]", "\\p{L}{3}", "[0-9]\\+"}) {
            assertNull(FormatoExpresion.mascara(patron), patron);
        }
    }

    @Test
    void patronesSinRestriccionNoImponenMascara() {
        for (String patron : new String[]{null, "", " ", "."}) {
            assertNull(FormatoExpresion.mascara(patron));
            assertTrue(FormatoExpresion.coincide("texto libre", patron));
        }
    }

    @Test
    void expresionInvalidaNoSeConvierteNiSeAceptaComoValida() {
        assertNull(FormatoExpresion.mascara("["));
        assertThrows(PatternSyntaxException.class, () -> FormatoExpresion.coincide("123", "["));
    }

    @Test
    void mascaraAcotadaYValidacionExacta() {
        assertNull(FormatoExpresion.mascara("[0-9]{256}"));
        assertNull(FormatoExpresion.mascara("[0-9]{999999999}"));
        assertEquals(255, FormatoExpresion.mascara("[0-9]{255}").length());
        assertTrue(FormatoExpresion.coincide(" 12345678-9 ", "[0-9]{8}-[0-9]"));
        assertFalse(FormatoExpresion.coincide("123456789", "[0-9]{8}-[0-9]"));
    }
}
