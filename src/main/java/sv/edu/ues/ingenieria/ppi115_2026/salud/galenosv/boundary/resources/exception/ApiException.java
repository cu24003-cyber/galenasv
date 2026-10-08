package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Error del contrato HTTP cuyo mensaje es seguro para el cliente. */
public class ApiException extends WebApplicationException {
    private final String code;

    public ApiException(Response.Status status, String code, String message) {
        super(message, status);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
