package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider;

import jakarta.ejb.EJBAccessException;
import jakarta.ejb.EJBException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.TransactionalException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.ApiError;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;

/** Traduce excepciones sin filtrar al cliente SQL, trazas ni causas internas. */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());
    private static final Set<String> NOT_FOUND = Set.of("error.noExisteActualizar", "error.noExisteEliminar",
            "procedimiento.noExiste");
    private static final Set<String> CONFLICT = Set.of("error.duplicado", "error.relacionado",
            "tipoDocumento.enUso", "tipoMedioContacto.enUso", "documento.duiUnico",
            "persona.eliminar.historial", "procedimiento.examenYaAsociado", "consulta.pacienteEnCurso");

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        return response(exception, uriInfo);
    }

    static Response response(Exception original, UriInfo uriInfo) {
        Throwable error = unwrap(original);
        int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        String code = "error.interno";
        String message = "Ocurrió un error interno al procesar la solicitud.";
        List<ApiError.Detail> details = List.of();
        Response.ResponseBuilder builder = null;

        if (error instanceof WebApplicationException web) {
            status = web.getResponse().getStatus();
            if (status < 400) {
                return web.getResponse();
            }
            // Conserva Allow, WWW-Authenticate, Retry-After y demás cabeceras del runtime.
            builder = Response.fromResponse(web.getResponse());
            code = "http." + status;
            message = httpMessage(status);
            if (web instanceof ApiException api) {
                code = api.getCode();
                message = api.getMessage();
            }
        } else if (error instanceof ConstraintViolationException validation) {
            boolean returnValue = validation.getConstraintViolations().stream().anyMatch(v -> {
                for (var node : v.getPropertyPath()) {
                    if (node.getKind() == ElementKind.RETURN_VALUE) return true;
                }
                return false;
            });
            if (!returnValue) {
                status = Response.Status.BAD_REQUEST.getStatusCode();
                code = "validacion.invalida";
                message = "Los datos de la solicitud no son válidos.";
                details = validation.getConstraintViolations().stream()
                        .map(v -> new ApiError.Detail(v.getPropertyPath().toString(), v.getMessage()))
                        .sorted(Comparator.comparing(ApiError.Detail::getField)
                                .thenComparing(ApiError.Detail::getMessage)).toList();
            }
        } else if (error instanceof EJBAccessException) {
            status = Response.Status.FORBIDDEN.getStatusCode();
            code = "http.403";
            message = httpMessage(status);
        } else if (error instanceof ServiceException service) {
            code = service.getMessageKey();
            if (NOT_FOUND.contains(code)) status = Response.Status.NOT_FOUND.getStatusCode();
            else if (CONFLICT.contains(code)) status = Response.Status.CONFLICT.getStatusCode();
            else if (service.getCause() == null) status = Response.Status.BAD_REQUEST.getStatusCode();
            if (status < 500) message = service.getMessage();
        } else if (error instanceof EntityExistsException || error instanceof OptimisticLockException) {
            status = Response.Status.CONFLICT.getStatusCode();
            code = "recurso.conflicto";
            message = "El registro ya existe o fue modificado por otra solicitud.";
        } else if (error instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            code = "solicitud.invalida";
            message = "Los datos de la solicitud no son válidos.";
        }

        // También cubre restricciones detectadas al confirmar la transacción EJB/JTA.
        SQLException sql = findSqlException(error);
        if (sql != null) {
            String state = sql.getSQLState();
            if ("23505".equals(state) || "23503".equals(state)) {
                status = Response.Status.CONFLICT.getStatusCode();
                code = "recurso.conflicto";
                message = "El registro está duplicado o tiene relaciones que impiden la operación.";
            } else if ("23514".equals(state) || "23502".equals(state)) {
                status = Response.Status.BAD_REQUEST.getStatusCode();
                code = "datos.invalidos";
                message = "Los datos no cumplen las restricciones del registro.";
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            }
        }
        if (status >= 500) {
            code = "error.interno";
            message = "Ocurrió un error interno al procesar la solicitud.";
            details = List.of();
            LOG.log(Level.SEVERE, "Error REST en " + path(uriInfo), original);
        }
        ApiError body = new ApiError(status, code, message, path(uriInfo), details);
        return (builder == null ? Response.status(status) : builder.status(status))
                .type(MediaType.APPLICATION_JSON_TYPE).entity(body).build();
    }

    private static Throwable unwrap(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while ((error instanceof EJBException && !(error instanceof EJBAccessException)
                || error instanceof TransactionalException) && error.getCause() != null && seen.add(error)) {
            error = error.getCause();
        }
        return error;
    }

    private static SQLException findSqlException(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (error != null && seen.add(error)) {
            if (error instanceof SQLException sql) return sql;
            error = error.getCause();
        }
        return null;
    }

    private static String path(UriInfo uriInfo) {
        return uriInfo == null ? "" : uriInfo.getRequestUri().getPath();
    }

    private static String httpMessage(int status) {
        return switch (status) {
            case 400 -> "La solicitud no es válida.";
            case 401 -> "Se requiere autenticación para acceder al recurso.";
            case 403 -> "No tiene permiso para acceder al recurso.";
            case 404 -> "El recurso no existe.";
            case 405 -> "El método HTTP no está permitido para este recurso.";
            case 406 -> "El formato solicitado no está disponible; utilice application/json.";
            case 409 -> "La operación entra en conflicto con el estado del recurso.";
            case 415 -> "El formato del cuerpo no está soportado; utilice application/json.";
            case 429 -> "Se ha superado el límite de solicitudes.";
            case 503 -> "El servicio no está disponible temporalmente.";
            default -> "No se pudo procesar la solicitud.";
        };
    }
}
