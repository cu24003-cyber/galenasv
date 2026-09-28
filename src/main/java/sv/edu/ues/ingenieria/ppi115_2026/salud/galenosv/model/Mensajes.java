package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

/** Acceso al mismo bundle y locale utilizados por las vistas. */
public final class Mensajes {
    private Mensajes() { }

    public static String texto(String clave) {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().getResourceBundle(context, "msg").getString(clave);
    }

    public static String detalle(RuntimeException exception) {
        java.util.logging.Logger.getLogger(Mensajes.class.getName())
                .log(java.util.logging.Level.WARNING, "Error de servicio en la vista", exception);
        return texto(exception instanceof ServiceException service
                ? service.getMessageKey() : "error.general");
    }
}
