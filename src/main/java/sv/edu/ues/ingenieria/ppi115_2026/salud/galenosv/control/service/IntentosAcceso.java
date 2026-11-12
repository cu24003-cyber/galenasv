package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.Duration;
import java.util.LinkedHashMap;

/**
 * Límite de solicitudes por dirección IP en ventanas de cinco minutos, en memoria y por
 * instancia. Cada ámbito lleva su propio contador: buscar citas no bloquea el login.
 */
@ApplicationScoped
public class IntentosAcceso {
    public enum Ambito {
        LOGIN(10, "auth.demasiadosIntentos", "Espere antes de volver a iniciar sesión."),
        /** HU1: clinicas, citas y ticket comparten este contador para impedir enumerar documentos. */
        BUSQUEDA_PUBLICA(60, "publico.demasiadasSolicitudes", "Espere unos minutos antes de volver a buscar.");

        final int maximo; final String codigo; final String mensaje;
        Ambito(int maximo, String codigo, String mensaje) { this.maximo = maximo; this.codigo = codigo; this.mensaje = mensaje; }
    }

    private record Ventana(Instant inicio, int intentos) {}
    private final LinkedHashMap<String, Ventana> ventanas = new LinkedHashMap<>();

    /** Login (se conserva la firma original). */
    public void registrar(String direccion) { registrar(Ambito.LOGIN, direccion, Instant.now()); }

    public void registrar(Ambito ambito, String direccion) { registrar(ambito, direccion, Instant.now()); }

    void registrar(String direccion, Instant ahora) { registrar(Ambito.LOGIN, direccion, ahora); }

    synchronized void registrar(Ambito ambito, String direccion, Instant ahora) {
        ventanas.values().removeIf(v -> !ahora.isBefore(v.inicio().plus(Duration.ofMinutes(5))));
        String clave = ambito.name() + ":" + direccion;
        Ventana previa = ventanas.get(clave);
        if (previa != null && previa.intentos() >= ambito.maximo) {
            throw new AccesoException(429, ambito.codigo, ambito.mensaje);
        }
        if (previa == null && ventanas.size() >= 10000) {
            throw new AccesoException(429, ambito.codigo, ambito.mensaje);
        }
        ventanas.put(clave, new Ventana(previa == null ? ahora : previa.inicio(), previa == null ? 1 : previa.intentos() + 1));
    }
}
