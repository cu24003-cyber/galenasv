package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider;

import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;

/** Los errores al leer JSON son 400; los errores al producir una respuesta siguen siendo 500. */
@Provider
public class JsonBodyReaderInterceptor implements ReaderInterceptor {
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        try {
            return context.proceed();
        } catch (JsonbException | JsonParsingException e) {
            throw new ApiException(Response.Status.BAD_REQUEST, "json.invalido",
                    "El JSON no es válido; revise la estructura y los tipos de los campos.");
        } catch (ProcessingException e) {
            Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            Throwable cause = e.getCause();
            while (cause != null && seen.add(cause)) {
                if (cause instanceof JsonbException || cause instanceof JsonParsingException) {
                    throw new ApiException(Response.Status.BAD_REQUEST, "json.invalido",
                            "El JSON no es válido; revise la estructura y los tipos de los campos.");
                }
                cause = cause.getCause();
            }
            throw e;
        }
    }
}
