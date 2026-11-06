package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class AccesoException extends ServiceException {
    private final int status;
    public AccesoException(int status, String code, String message) {
        super(code, message, null);
        this.status = status;
    }
    public int getStatus() { return status; }
}
