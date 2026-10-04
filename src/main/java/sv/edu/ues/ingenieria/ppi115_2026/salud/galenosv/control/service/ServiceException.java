package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class ServiceException extends RuntimeException {
    private String messageKey = "error.general";
    private Object[] messageArguments = new Object[0];

    private ServiceException(String messageKey, String message, Object[] arguments) {
        super(message);
        this.messageKey = messageKey;
        this.messageArguments = arguments == null ? new Object[0] : arguments.clone();
    }

    public static ServiceException localizada(String messageKey, String message, Object... arguments) {
        return new ServiceException(messageKey, message, arguments);
    }

    public ServiceException(String messageKey, String message, Throwable cause) {
        super(message, cause);
        this.messageKey = messageKey;
    }

    public String getMessageKey() { return messageKey; }
    public Object[] getMessageArguments() { return messageArguments.clone(); }


    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
