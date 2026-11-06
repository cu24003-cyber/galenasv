package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@SessionScoped
public class AccesoSesion implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID idUsuario;
    private String csrfToken;
    private Instant inicio;

    public void autenticar(UUID idUsuario) {
        this.idUsuario = java.util.Objects.requireNonNull(idUsuario);
        inicio = Instant.now();
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean vigente() {
        return idUsuario != null && inicio != null && Instant.now().isBefore(inicio.plus(Duration.ofHours(8)));
    }
    public UUID getIdUsuario() { return idUsuario; }
    public String getCsrfToken() { return csrfToken; }
}
