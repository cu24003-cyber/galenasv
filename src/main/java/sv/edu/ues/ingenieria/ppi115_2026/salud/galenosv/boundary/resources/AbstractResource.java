package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.IdentifiableDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AbstractService;

/** CRUD común; la transacción incluye la resolución de relaciones y el mapeo a DTO. */
@Transactional(rollbackOn = Exception.class)
public abstract class AbstractResource<T, D extends IdentifiableDto> {

    protected abstract AbstractService<T, UUID> getService();
    protected abstract T nuevaEntidad(UUID id);
    protected abstract D toDto(T entidad);
    protected abstract void aplicar(D datos, T entidad);

    @GET
    public Response listar() {
        return Response.ok(getService().listarTodos().stream().map(this::toDto).toList()).build();
    }

    @GET
    @Path("{id}")
    public Response buscar(@PathParam("id") String id) {
        return Response.ok(toDto(buscarExistente(parseId(id)))).build();
    }

    @POST
    public Response crear(@Valid @NotNull(message = "El cuerpo JSON es obligatorio.") D datos,
            @Context UriInfo uriInfo) {
        exigirCuerpo(datos);
        if (datos.id() != null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "id.generado",
                    "El identificador se genera en el servidor; omítalo al crear.");
        }
        UUID id = UUID.randomUUID();
        T entidad = nuevaEntidad(id);
        aplicar(datos, entidad);
        getService().crear(entidad);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(id.toString()).build())
                .entity(toDto(entidad)).build();
    }

    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id") String id,
            @Valid @NotNull(message = "El cuerpo JSON es obligatorio.") D datos) {
        exigirCuerpo(datos);
        UUID identificador = parseId(id);
        if (datos.id() != null && !identificador.equals(datos.id())) {
            throw new ApiException(Response.Status.BAD_REQUEST, "id.inconsistente",
                    "El identificador del cuerpo debe coincidir con el de la URL.");
        }
        T entidad = buscarExistente(identificador);
        aplicar(datos, entidad);
        getService().actualizar(entidad);
        return Response.ok(toDto(entidad)).build();
    }

    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") String id) {
        UUID identificador = parseId(id);
        buscarExistente(identificador);
        getService().eliminar(identificador);
        return Response.noContent().build();
    }

    protected <R> R relacion(AbstractService<R, UUID> service, UUID id, String campo) {
        if (id == null) {
            return null;
        }
        R entidad = service.buscarPorId(id);
        if (entidad == null) {
            throw new ApiException(Response.Status.NOT_FOUND, "relacion.noExiste",
                    "No existe el registro relacionado indicado en " + campo + ".");
        }
        return entidad;
    }

    private T buscarExistente(UUID id) {
        T entidad = getService().buscarPorId(id);
        if (entidad == null) {
            throw new ApiException(Response.Status.NOT_FOUND, "recurso.noExiste", "El recurso no existe.");
        }
        return entidad;
    }

    private UUID parseId(String id) {
        try {
            UUID parsed = UUID.fromString(id);
            if (!parsed.toString().equalsIgnoreCase(id)) {
                throw new IllegalArgumentException();
            }
            return parsed;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(Response.Status.BAD_REQUEST, "id.invalido",
                    "El identificador debe ser un UUID válido.");
        }
    }

    private void exigirCuerpo(D datos) {
        if (datos == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "cuerpo.requerido",
                    "El cuerpo JSON es obligatorio.");
        }
    }
}
