package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.stream.JsonParser.Event;
import java.io.StringReader;
import java.util.UUID;

/** Formato de compatibilidad para la columna text del esquema original. */
public record ContenidoOrden(UUID idExamen, String indicaciones) {
    public static final String PREFIJO = "@galenosv/orden/v1:";

    public static String prefijo(UUID idExamen) {
        return PREFIJO + (idExamen == null ? "-" : idExamen) + ":";
    }

    public static String guardar(UUID idExamen, String texto) {
        var json = Json.createObjectBuilder();
        if (texto == null) json.addNull("indicaciones");
        else json.add("indicaciones", texto);
        return prefijo(idExamen) + json.build();
    }

    public static ContenidoOrden leer(String almacenado) {
        ContenidoOrden contenido = decodificar(almacenado);
        return contenido == null ? new ContenidoOrden(null, almacenado) : contenido;
    }

    public static boolean estructurado(String almacenado) {
        return decodificar(almacenado) != null;
    }

    private static ContenidoOrden decodificar(String almacenado) {
        if (almacenado != null && almacenado.startsWith(PREFIJO)) {
            int finId = almacenado.indexOf(':', PREFIJO.length());
            if (finId > 0) {
                try (var lector = Json.createParser(new StringReader(almacenado.substring(finId + 1)))) {
                    String id = almacenado.substring(PREFIJO.length(), finId);
                    UUID uuid = "-".equals(id) ? null : UUID.fromString(id);
                    if (uuid != null && !uuid.toString().equals(id)) return null;
                    if (!lector.hasNext() || lector.next() != Event.START_OBJECT
                            || !lector.hasNext() || lector.next() != Event.KEY_NAME
                            || !"indicaciones".equals(lector.getString()) || !lector.hasNext()) return null;
                    Event valor = lector.next();
                    if (valor != Event.VALUE_NULL && valor != Event.VALUE_STRING) return null;
                    String texto = valor == Event.VALUE_NULL ? null : lector.getString();
                    if (!lector.hasNext() || lector.next() != Event.END_OBJECT || lector.hasNext()) return null;
                    return new ContenidoOrden(uuid, texto);
                } catch (JsonException | IllegalArgumentException ex) {
                    // Texto legado o dañado: conservarlo íntegro, sin inventar un examen.
                }
            }
        }
        return null;
    }
}
