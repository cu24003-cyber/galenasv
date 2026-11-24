package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.stream.JsonParser.Event;
import java.io.StringReader;

/** Conserva atención y retrocesos en la columna valor del parche original. */
public record ContenidoPaso(String valor, String motivoRetroceso) {
    public static final String PREFIJO = "@galenosv/paso/v1:";

    public static String guardar(String valor, String motivo) {
        if (motivo == null && (valor == null || !valor.startsWith("@galenosv/paso/"))) return valor;
        var json = Json.createObjectBuilder();
        if (valor == null) json.addNull("valor"); else json.add("valor", valor);
        if (motivo == null) json.addNull("motivoRetroceso"); else json.add("motivoRetroceso", motivo);
        return PREFIJO + json.build();
    }

    public static ContenidoPaso leer(String almacenado) {
        if (almacenado != null && almacenado.startsWith(PREFIJO)) {
            try (var lector = Json.createParser(new StringReader(almacenado.substring(PREFIJO.length())))) {
                if (lector.hasNext() && lector.next() == Event.START_OBJECT) {
                    String[] textos = new String[2];
                    String[] claves = {"valor", "motivoRetroceso"};
                    for (int i = 0; i < claves.length; i++) {
                        if (!lector.hasNext() || lector.next() != Event.KEY_NAME
                                || !claves[i].equals(lector.getString()) || !lector.hasNext())
                            return new ContenidoPaso(almacenado, null);
                        Event tipo = lector.next();
                        if (tipo != Event.VALUE_NULL && tipo != Event.VALUE_STRING)
                            return new ContenidoPaso(almacenado, null);
                        textos[i] = tipo == Event.VALUE_NULL ? null : lector.getString();
                    }
                    if (lector.hasNext() && lector.next() == Event.END_OBJECT && !lector.hasNext())
                        return new ContenidoPaso(textos[0], textos[1]);
                }
            } catch (JsonException ex) {
                // Un texto legado o dañado se conserva íntegro, sin inventar un motivo.
            }
        }
        return new ContenidoPaso(almacenado, null);
    }
}
