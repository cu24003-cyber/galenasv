package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import jakarta.ejb.Stateless;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.CitaPublicaService.CitaPublica;

/**
 * HU1: ticket PDF con el mismo resumen que la card de la cita (OpenPDF 2.4.0). Recibe solo
 * CitaPublica, que no tiene nombre, documento, notas, valores, responsables ni resultados.
 */
@Stateless
public class TicketCitaService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generar(CitaPublica cita) {
        var salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A6, 24, 24, 28, 28);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Ticket de cita");
            documento.addCreator("GalenoSV");
            documento.open();
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font texto = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font nota = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

            documento.add(centrado("GalenoSV", titulo));
            documento.add(centrado("Ticket de cita", subtitulo));
            documento.add(linea("Código de cita: " + cita.idConsulta(), texto, 10));
            documento.add(linea("Fecha y hora: " + cita.fechaInicio().atZoneSameInstant(CitaPublicaService.ZONA).format(FECHA)
                    + " (hora de El Salvador)", texto, 4));
            documento.add(linea("Clínica: " + cita.nombreClinica(), texto, 4));
            documento.add(linea("Procedimientos:", texto, 4));
            if (cita.procedimientos().isEmpty()) {
                documento.add(linea("Sin procedimientos registrados", texto, 2));
            } else {
                for (String procedimiento : cita.procedimientos()) documento.add(linea("- " + procedimiento, texto, 2));
            }
            documento.add(linea("Presente este ticket en la recepción de la clínica.", nota, 14));
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el ticket de la cita.", e);
        } finally {
            if (documento.isOpen()) documento.close();
        }
        return salida.toByteArray();
    }

    public static String nombreArchivo(UUID idConsulta) {
        return "ticket-cita-" + idConsulta.toString().substring(0, 8) + ".pdf";
    }

    private static Paragraph centrado(String valor, Font fuente) {
        Paragraph p = new Paragraph(valor, fuente);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static Paragraph linea(String valor, Font fuente, float espacioAntes) {
        Paragraph p = new Paragraph(valor, fuente);
        p.setSpacingBefore(espacioAntes);
        return p;
    }
}
