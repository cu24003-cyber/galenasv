package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
