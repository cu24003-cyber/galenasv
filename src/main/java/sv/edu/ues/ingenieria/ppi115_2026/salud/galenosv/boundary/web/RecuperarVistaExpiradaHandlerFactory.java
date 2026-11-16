package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.web;

import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import java.io.IOException;
import java.util.Iterator;

/** Recupera los POST que llegan con el estado de una vista JSF ya vencido. */
public class RecuperarVistaExpiradaHandlerFactory extends ExceptionHandlerFactory {
    private final ExceptionHandlerFactory wrapped;

    public RecuperarVistaExpiradaHandlerFactory(ExceptionHandlerFactory wrapped) {
        this.wrapped = wrapped;
    }

    @Override public ExceptionHandler getExceptionHandler() {
        return new RecuperarVistaExpiradaHandler(wrapped.getExceptionHandler());
    }

    static boolean contieneVistaExpirada(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ViewExpiredException) return true;
        }
        return false;
    }

    private static final class RecuperarVistaExpiradaHandler extends ExceptionHandlerWrapper {
        private final ExceptionHandler wrapped;

        RecuperarVistaExpiradaHandler(ExceptionHandler wrapped) {
            this.wrapped = wrapped;
        }

        @Override public ExceptionHandler getWrapped() { return wrapped; }

        @Override public void handle() throws FacesException {
            Iterator<ExceptionQueuedEvent> eventos = getUnhandledExceptionQueuedEvents().iterator();
            while (eventos.hasNext()) {
                ExceptionQueuedEvent evento = eventos.next();
                if (contieneVistaExpirada(evento.getContext().getException())) {
                    eventos.remove();
                    FacesContext faces = FacesContext.getCurrentInstance();
                    String destino = faces.getExternalContext().getRequestContextPath() + "/paginas/consultas.xhtml";
                    try {
                        faces.getExternalContext().redirect(destino);
                        faces.responseComplete();
                    } catch (IOException e) {
                        throw new FacesException(e);
                    }
                    return;
                }
            }
            wrapped.handle();
        }
    }
}
