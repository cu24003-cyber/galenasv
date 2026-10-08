package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import java.time.Instant;
import java.util.List;

/** Contrato común para respuestas HTTP de error. */
public class ApiError {
    private final int status;
    private final String code;
    private final String message;
    private final String path;
    private final String timestamp;
    private final List<Detail> details;

    public ApiError(int status, String code, String message, String path, List<Detail> details) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now().toString();
        this.details = List.copyOf(details);
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public String getTimestamp() { return timestamp; }
    public List<Detail> getDetails() { return details; }

    public static class Detail {
        private final String field;
        private final String message;

        public Detail(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
    }
}
