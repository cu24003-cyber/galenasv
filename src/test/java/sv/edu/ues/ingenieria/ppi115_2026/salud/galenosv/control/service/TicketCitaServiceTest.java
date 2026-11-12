package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.CitaPublicaService.CitaPublica;

class TicketCitaServiceTest {
    private final TicketCitaService servicio = new TicketCitaService();
    private final UUID id = UUID.fromString("de110009-0000-4000-8000-000000000002");

    private static List<String> lineas(byte[] pdf) throws Exception {
        try (PdfReader lector = new PdfReader(pdf)) {
            assertEquals(1, lector.getNumberOfPages());
            return Arrays.stream(new PdfTextExtractor(lector).getTextFromPage(1).split("\\R"))
                    .map(String::strip).filter(l -> !l.isEmpty()).toList();
        }
    }

    @Test void elTicketContieneExactamenteElResumenDeLaCita() throws Exception {
        var cita = new CitaPublica(id, OffsetDateTime.parse("2026-11-12T14:00Z"), "Clínica de demostración",
                List.of("Consulta general de demostración", "Curación"));
        byte[] pdf = servicio.generar(cita);
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
        assertEquals(List.of(
                "GalenoSV",
                "Ticket de cita",
                "Código de cita: de110009-0000-4000-8000-000000000002",
                "Fecha y hora: 12/11/2026 08:00 (hora de El Salvador)",
                "Clínica: Clínica de demostración",
                "Procedimientos:",
                "- Consulta general de demostración",
                "- Curación",
                "Presente este ticket en la recepción de la clínica."), lineas(pdf));
    }

    @Test void sinProcedimientosLoIndica() throws Exception {
        var cita = new CitaPublica(id, OffsetDateTime.parse("2026-11-19T16:00Z"), "Clínica Norte", List.of());
        assertTrue(lineas(servicio.generar(cita)).contains("Sin procedimientos registrados"));
    }

    @Test void nombreDeArchivoUsaElInicioDelCodigo() {
        assertEquals("ticket-cita-de110009.pdf", TicketCitaService.nombreArchivo(id));
    }
}
