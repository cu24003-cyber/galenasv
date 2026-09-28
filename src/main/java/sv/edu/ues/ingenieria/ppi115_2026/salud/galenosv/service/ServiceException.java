package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class ServiceException extends RuntimeException {
    private String messageKey = "error.general";

    public ServiceException(String messageKey, String message, Throwable cause) {
        super(message, cause);
        this.messageKey = messageKey;
    }

    public String getMessageKey() { return messageKey; }


    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
