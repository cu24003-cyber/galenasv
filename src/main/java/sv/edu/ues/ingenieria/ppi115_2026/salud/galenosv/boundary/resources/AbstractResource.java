package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.PatternSyntaxException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.FormatoExpresion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.IdentifiableDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.exception.ApiException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AbstractService;

/** CRUD común; la transacción incluye la resolución de relaciones y el mapeo a DTO. */
@Transactional(rollbackOn = Exception.class)
public abstract class AbstractResource<T, D extends IdentifiableDto> {

    @Inject
    private Validator validator;

    private UUID padre;
    private ColeccionRelacionada contextoColeccion;
    private boolean modificacionParcial;

    protected abstract AbstractService<T, UUID> getService();
    protected abstract T nuevaEntidad(UUID id);
    protected abstract D toDto(T entidad);
    protected abstract void aplicar(D datos, T entidad);

    /** Nombre público del filtro y ruta JPA; nunca se aceptan rutas JPA del cliente. */
    protected Map<String, String> filtrosPermitidos() {
        return Map.of();
    }

    protected static final Set<String> CRUD = Set.of("POST", "PUT", "PATCH", "DELETE");

    protected record ColeccionRelacionada(Class<? extends AbstractResource<?, ?>> recurso,
            String filtro, Set<String> escrituras) {
        protected ColeccionRelacionada(Class<? extends AbstractResource<?, ?>> recurso, String filtro) {
            this(recurso, filtro, Set.of());
        }
    }

    protected Set<String> metodosEscritura() { return CRUD; }
    protected Set<String> camposInmutables() { return Set.of(); }

    protected String filtroPadre() { return contextoColeccion == null ? null : contextoColeccion.filtro(); }
    protected UUID idPadre() { return padre; }
    protected boolean esModificacionParcial() { return modificacionParcial; }

    protected Map<String, ColeccionRelacionada> coleccionesRelacionadas() {
        return Map.of();
    }

    @GET
    public Response listar(@Context UriInfo uriInfo) {
        return listarFiltrado(uriInfo, contextoColeccion == null ? Map.of() : Map.of(filtroPadre(), padre));
    }

    @Path("{id}/{coleccion}")
    public AbstractResource<?, ?> relacionados(@PathParam("id") String id,
            @PathParam("coleccion") String coleccion,
            @Context ResourceContext resourceContext) {
        ColeccionRelacionada relacionada = coleccionesRelacionadas().get(coleccion);
        if (relacionada == null) {
            throw new ApiException(Response.Status.NOT_FOUND, "recurso.noExiste", "El recurso no existe.");
        }
        UUID identificador = parseId(id);
        buscarExistente(identificador);
        AbstractResource<?, ?> recurso = resourceContext.getResource(relacionada.recurso());
        recurso.padre = identificador;
        recurso.contextoColeccion = relacionada;
        return recurso;
    }

    private Response listarFiltrado(UriInfo uriInfo, Map<String, UUID> filtrosFijos) {
        Map<String, String> permitidos = filtrosPermitidos();
        Map<String, UUID> filtros = new LinkedHashMap<>(filtrosFijos);
        for (var parametro : uriInfo.getQueryParameters().entrySet()) {
            String nombre = parametro.getKey();
            if (!permitidos.containsKey(nombre)) {
                throw new ApiException(Response.Status.BAD_REQUEST, "filtro.desconocido",
                        "El filtro " + nombre + " no está permitido para este recurso.");
            }
            if (parametro.getValue().size() != 1) {
                throw new ApiException(Response.Status.BAD_REQUEST, "filtro.repetido",
                        "Cada filtro debe enviarse una sola vez.");
            }
            UUID valor = parseId(parametro.getValue().getFirst());
            if (filtrosFijos.containsKey(nombre) && !filtrosFijos.get(nombre).equals(valor)) {
                throw new ApiException(Response.Status.BAD_REQUEST, "filtro.inconsistente",
                        "El filtro " + nombre + " debe coincidir con el identificador de la URL.");
            }
            filtros.put(nombre, valor);
        }
        Map<String, UUID> atributos = new LinkedHashMap<>();
        filtros.forEach((nombre, valor) -> atributos.put(permitidos.get(nombre), valor));
        var entidades = atributos.isEmpty() ? getService().listarTodos() : getService().listarPorFiltros(atributos);
        return Response.ok(entidades.stream().map(this::toDto).toList()).build();
    }

    @GET
    @Path("{id}")
    public Response buscar(@PathParam("id") String id) {
        return Response.ok(toDto(buscarExistente(parseId(id)))).build();
    }

    @POST
    public Response crear(@NotNull(message = "El cuerpo JSON es obligatorio.") D datos,
            @Context UriInfo uriInfo) {
        exigirMetodo("POST", false);
        exigirCuerpo(datos);
        datos = prepararEnColeccion(datos);
        validarDto(datos);
        if (datos.id() != null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "id.generado",
                    "El identificador se genera en el servidor; omítalo al crear.");
        }
        UUID id = UUID.randomUUID();
        T entidad = crearEntidad(id, datos);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(toDto(entidad).id().toString()).build())
                .entity(toDto(entidad)).build();
    }

    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id") String id,
            @NotNull(message = "El cuerpo JSON es obligatorio.") D datos) {
        exigirMetodo("PUT", true);
        exigirCuerpo(datos);
        UUID identificador = parseId(id);
        if (datos.id() != null && !identificador.equals(datos.id())) {
            throw new ApiException(Response.Status.BAD_REQUEST, "id.inconsistente",
                    "El identificador del cuerpo debe coincidir con el de la URL.");
        }
        T entidad = buscarExistente(identificador);
        datos = prepararEnColeccion(datos);
        validarDto(datos);
        actualizarEntidad(entidad, datos);
        return Response.ok(toDto(entidad)).build();
    }

    @PATCH
    @Path("{id}")
    @Consumes({"application/json", "application/merge-patch+json"})
    public Response modificar(@PathParam("id") String id, String cuerpo) {
        exigirMetodo("PATCH", true);
        T entidad = buscarExistente(parseId(id));
        JsonObject cambios = leerObjeto(cuerpo);
        if (cambios.keySet().stream().anyMatch(camposInmutables()::contains)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "campos.servidor", "El JSON intenta modificar campos controlados por el flujo.");
        }
        D actual = toDto(entidad);
        Set<String> campos = Arrays.stream(actual.getClass().getDeclaredFields())
                .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .map(java.lang.reflect.Field::getName).collect(Collectors.toSet());
        if (!campos.containsAll(cambios.keySet())) {
            throw new ApiException(Response.Status.BAD_REQUEST, "campo.desconocido", "El JSON contiene campos desconocidos.");
        }
        JsonObject combinado = (JsonObject) Json.createMergePatch(cambios).apply(jsonDto(actual));
        D datos = prepararEnColeccion(convertir(combinado, actual));
        if (datos.id() == null || !datos.id().equals(actual.id())) {
            throw new ApiException(Response.Status.BAD_REQUEST, "id.inconsistente", "El identificador no se puede modificar.");
        }
        validarDto(datos);
        modificacionParcial = true;
        actualizarEntidad(entidad, datos);
        return Response.ok(toDto(entidad)).build();
    }

    @OPTIONS
    public Response opcionesColeccion() { return Response.ok().allow(permitidos(false)).build(); }

    @OPTIONS
    @Path("{id}")
    public Response opcionesEntidad() { return Response.ok().allow(permitidos(true)).build(); }

    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") String id) {
        exigirMetodo("DELETE", true);
        UUID identificador = parseId(id);
        T entidad = buscarExistente(identificador);
        eliminarEntidad(entidad);
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

    protected T buscarExistente(UUID id) {
        T entidad = getService().buscarPorId(id);
        if (entidad == null || (contextoColeccion != null && !getService().perteneceAFiltros(id,
                Map.of(filtrosPermitidos().get(filtroPadre()), padre)))) {
            throw new ApiException(Response.Status.NOT_FOUND, "recurso.noExiste", "El recurso no existe.");
        }
        return entidad;
    }

    protected UUID parseId(String id) {
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

    protected T crearEntidad(UUID id, D datos) {
        T entidad = nuevaEntidad(id);
        aplicar(datos, entidad);
        getService().crear(entidad);
        return entidad;
    }

    protected void actualizarEntidad(T entidad, D datos) {
        aplicar(datos, entidad);
        getService().actualizar(entidad);
    }

    protected void eliminarEntidad(T entidad) { getService().eliminar(toDto(entidad).id()); }

    protected String validarValorCatalogo(String valor, Boolean activo, String expresion) {
        if (!Boolean.TRUE.equals(activo)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "registro.seleccionInvalida", "Seleccione un tipo activo.");
        }
        try {
            if (!FormatoExpresion.coincide(valor, expresion)) {
                throw new ApiException(Response.Status.BAD_REQUEST, "registro.formatoInvalido", "El valor no cumple el formato del tipo seleccionado.");
            }
        } catch (PatternSyntaxException e) {
            throw new ApiException(Response.Status.BAD_REQUEST, "registro.formatoConfiguracionInvalida", "El formato del tipo seleccionado no es válido.");
        }
        return valor.trim();
    }

    protected D prepararEnColeccion(D datos) {
        if (contextoColeccion == null) return datos;
        String campo = filtroPadre();
        JsonObject json = jsonDto(datos);
        if (json.containsKey(campo) && !json.isNull(campo)
                && !padre.equals(parseId(json.getString(campo)))) {
            throw new ApiException(Response.Status.BAD_REQUEST, "relacion.inconsistente",
                    "El campo " + campo + " debe coincidir con el padre de la URL.");
        }
        return convertir(Json.createObjectBuilder(json).add(campo, padre.toString()).build(), datos);
    }

    private void validarDto(D datos) {
        var errores = validator.validate(datos);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
    }

    private Set<String> permitidos(boolean individual) {
        Set<String> resultado = new LinkedHashSet<>(Set.of("GET", "HEAD", "OPTIONS"));
        for (String metodo : metodosEscritura()) {
            if (contextoColeccion != null && !contextoColeccion.escrituras().contains(metodo)) continue;
            if (individual != metodo.equals("POST")) resultado.add(metodo);
        }
        return resultado;
    }

    private void exigirMetodo(String metodo, boolean individual) {
        if (!permitidos(individual).contains(metodo)) {
            throw new NotAllowedException(Response.status(Response.Status.METHOD_NOT_ALLOWED)
                    .allow(permitidos(individual)).build());
        }
    }

    private JsonObject leerObjeto(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "cuerpo.requerido", "El cuerpo JSON es obligatorio.");
        }
        try (var reader = Json.createReader(new StringReader(cuerpo))) {
            return reader.readObject();
        } catch (JsonException e) {
            throw jsonInvalido();
        }
    }

    private JsonObject jsonDto(D datos) { return leerObjeto(conJsonb(jsonb -> jsonb.toJson(datos))); }

    @SuppressWarnings("unchecked")
    private D convertir(JsonObject json, D modelo) {
        return (D) conJsonb(jsonb -> jsonb.fromJson(json.toString(), modelo.getClass()));
    }

    private <R> R conJsonb(Function<Jsonb, R> operacion) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return operacion.apply(jsonb);
        } catch (JsonbException e) {
            throw jsonInvalido();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo procesar el DTO.", e);
        }
    }

    private ApiException jsonInvalido() {
        return new ApiException(Response.Status.BAD_REQUEST, "json.invalido", "El JSON no es válido; revise la estructura y los tipos de los campos.");
    }
}
