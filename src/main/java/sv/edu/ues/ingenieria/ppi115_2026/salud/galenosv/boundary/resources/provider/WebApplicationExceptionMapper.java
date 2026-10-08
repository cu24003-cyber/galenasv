package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Cubre también errores de enrutamiento y negociación generados por Jakarta REST. */
@Provider
@Priority(Priorities.USER - 100)
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        return ApiExceptionMapper.response(exception, uriInfo);
    }
}
