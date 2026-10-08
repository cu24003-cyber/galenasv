package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider;

import jakarta.annotation.Priority;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER - 100)
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ValidationException exception) {
        return ApiExceptionMapper.response(exception, uriInfo);
    }
}
