package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.Duration;
import java.util.LinkedHashMap;

@ApplicationScoped
public class IntentosAcceso {
    private record Ventana(Instant inicio, int intentos) {}
    private final LinkedHashMap<String, Ventana> ventanas = new LinkedHashMap<>();

    public void registrar(String direccion) { registrar(direccion, Instant.now()); }

    synchronized void registrar(String direccion, Instant ahora) {
        ventanas.values().removeIf(v -> !ahora.isBefore(v.inicio().plus(Duration.ofMinutes(5))));
        Ventana previa = ventanas.get(direccion);
        if (previa != null && previa.intentos() >= 10) {
            throw new AccesoException(429, "auth.demasiadosIntentos", "Espere antes de volver a iniciar sesión.");
        }
        if (previa == null && ventanas.size() >= 10000) {
            throw new AccesoException(429, "auth.demasiadosIntentos", "Espere antes de volver a iniciar sesión.");
        }
        ventanas.put(direccion, new Ventana(previa == null ? ahora : previa.inicio(), previa == null ? 1 : previa.intentos() + 1));
    }
}
