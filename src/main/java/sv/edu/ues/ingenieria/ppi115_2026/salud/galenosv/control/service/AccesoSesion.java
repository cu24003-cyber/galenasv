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
    private UUID idPersona;
    private String csrfToken;
    private Instant inicio;

    public void autenticar(UUID idPersona) {
        this.idPersona = java.util.Objects.requireNonNull(idPersona);
        inicio = Instant.now();
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean vigente() {
        return idPersona != null && inicio != null && Instant.now().isBefore(inicio.plus(Duration.ofHours(8)));
    }
    public UUID getIdPersona() { return idPersona; }
    public String getCsrfToken() { return csrfToken; }
}
