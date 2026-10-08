package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.Id;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.ejb.EJBException;
import jakarta.ejb.EJBAccessException;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.AppConfig;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.ApiExceptionMapper;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.JsonBodyReaderInterceptor;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.ValidationExceptionMapper;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.WebApplicationExceptionMapper;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AbstractService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;

/** Prueba el contrato HTTP con Jakarta REST y JSON-B reales, sin tocar la base de datos. */
class RestApiTest extends JerseyTest {
    private static final UUID ID = UUID.fromString("6a4ed47b-8d96-4d83-bbe3-653026ae5120");
    private static final UUID MISSING = UUID.fromString("7a4ed47b-8d96-4d83-bbe3-653026ae5120");
    private static final String BASE_PACKAGE = "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv";
    private static final List<Class<?>> RESOURCES = List.of(
            ClinicaResource.class,
            ConsultaResource.class,
            ConsultaProcedimientoResource.class,
            ConsultaProcedimientoPasoResource.class,
            DocumentoResource.class,
            ExamenResource.class,
            ExamenResultadoResource.class,
            ExamenTipoExamenResource.class,
            MedioContactoResource.class,
            OrdenExamenResource.class,
            PersonaResource.class,
            PersonaRolResource.class,
            ProcedimientoResource.class,
            ProcedimientoPasoResource.class,
            ProcedimientoPasoExamenResource.class,
            ProcedimientoPasoSecuenciaResource.class,
            RolResource.class,
            TipoDocumentoResource.class,
            TipoExamenResource.class,
            TipoMedioContactoResource.class);
    private Map<Class<?>, AbstractService<Object, UUID>> services;
    private Map<Class<?>, Map<UUID, Object>> records;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Application configure() {
        services = new HashMap<>();
        records = new HashMap<>();
        ResourceConfig config = new ResourceConfig().property(ServerProperties.WADL_FEATURE_DISABLE, true);
        Map<Class, Object> bindings = new HashMap<>();
        for (Class<?> resource : RESOURCES) {
            config.register(resource);
            Class<?> entity = entityType(resource);
            try {
                Class serviceClass = Class.forName(BASE_PACKAGE + ".control.service." + entity.getSimpleName() + "Service");
                AbstractService<Object, UUID> service = (AbstractService<Object, UUID>) mock(serviceClass);
                Map<UUID, Object> data = new HashMap<>();
                data.put(ID, fixture(entity));
                records.put(entity, data);
                services.put(entity, service);
                bindings.put(serviceClass, service);
                when(service.listarTodos()).thenAnswer(i -> List.copyOf(data.values()));
                when(service.buscarPorId(any(UUID.class))).thenAnswer(i -> data.get(i.getArgument(0)));
                doAnswer(i -> {
                    Object value = i.getArgument(0);
                    data.put(identifier(value), value);
                    return null;
                }).when(service).crear(any());
                doAnswer(i -> {
                    Object value = i.getArgument(0);
                    data.put(identifier(value), value);
                    return null;
                }).when(service).actualizar(any());
                doAnswer(i -> {
                    data.remove(i.getArgument(0));
                    return null;
                }).when(service).eliminar(any());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindings.forEach((type, service) -> bind(service).to(type));
            }
        });
        return config.register(JakartaEE11Resource.class).register(ErrorsResource.class)
                .register(ApiExceptionMapper.class).register(WebApplicationExceptionMapper.class)
                .register(ValidationExceptionMapper.class).register(JsonBodyReaderInterceptor.class);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new InMemoryTestContainerFactory();
    }

    @Override
    protected URI getBaseUri() {
        return URI.create("http://localhost/api/");
    }

    @BeforeEach
    void iniciar() throws Exception {
        super.setUp();
    }

    @AfterEach
    void detener() throws Exception {
        super.tearDown();
    }

    static Stream<Class<?>> recursos() {
        return RESOURCES.stream();
    }

    @ParameterizedTest(name = "CRUD JSON: {0}")
    @MethodSource("recursos")
    void crudCompletoEnTodasLasEntidades(Class<?> resource) throws Exception {
        String route = route(resource);
        Class<?> entity = entityType(resource);
        String idField = idField(entity).getName();
        try (Response response = target(route).request().get()) {
            assertEquals(200, response.getStatus());
            assertEquals(1, Json.createReader(new StringReader(response.readEntity(String.class)))
                    .readArray().size());
        }
        UUID created;
        JsonObject body = payload(entity);
        try (Response response = target(route).request().post(Entity.json(body.toString()))) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
            JsonObject dto = json(response);
            created = UUID.fromString(dto.getString(idField));
            assertEquals(getBaseUri().resolve(route + "/" + created), response.getLocation());
            for (String field : body.keySet()) assertEquals(body.get(field), dto.get(field), field);
            assertFalse(dto.keySet().stream().anyMatch(k -> k.endsWith("Collection")));
            assertFalse(dto.containsKey("id"));
        }
        String path = route + "/" + created;
        try (Response response = target(path).request().get()) {
            assertEquals(200, response.getStatus());
            assertEquals(created.toString(), json(response).getString(idField));
        }
        JsonObject updated = updatedPayload(entity);
        try (Response response = target(path).request().put(Entity.json(updated.toString()))) {
            assertEquals(200, response.getStatus());
            JsonObject dto = json(response);
            assertEquals(created.toString(), dto.getString(idField));
            for (String field : updated.keySet()) assertEquals(updated.get(field), dto.get(field), field);
        }
        verify(services.get(entity)).actualizar(any());
        try (Response response = target(path).request().delete()) {
            assertEquals(204, response.getStatus());
            assertFalse(response.hasEntity());
        }
        try (Response response = target(path).request().get()) {
            assertError(response, 404);
        }
    }

    @ParameterizedTest(name = "Validación JSON: {0}")
    @MethodSource("recursos")
    void rechazaDatosIncompletosAntesDePersistir(Class<?> resource) {
        try (Response response = target(route(resource)).request().post(Entity.json("{}"))) {
            JsonObject error = assertError(response, 400);
            assertFalse(error.getJsonArray("details").isEmpty());
            verify(services.get(entityType(resource)), never()).crear(any());
        }
    }

    @ParameterizedTest(name = "404: {0}")
    @MethodSource("recursos")
    void noActualizaNiEliminaUnRecursoInexistente(Class<?> resource) throws Exception {
        String path = route(resource) + "/" + MISSING;
        try (Response response = target(path).request().put(Entity.json(payload(entityType(resource)).toString()))) {
            assertError(response, 404);
        }
        try (Response response = target(path).request().delete()) {
            assertError(response, 404);
        }
        verify(services.get(entityType(resource)), never()).actualizar(any());
        verify(services.get(entityType(resource)), never()).eliminar(any());
    }

    @Test
    void appConfigActivaLaRutaApi() {
        assertEquals("api", AppConfig.class.getAnnotation(jakarta.ws.rs.ApplicationPath.class).value());
        assertTrue(new AppConfig().getClasses().isEmpty());
    }

    @Test
    void listaVaciaDevuelve200ConArregloVacio() {
        records.get(entityType(ClinicaResource.class)).clear();
        try (Response response = target("clinicas").request().get()) {
            assertEquals(200, response.getStatus());
            assertEquals("[]", response.readEntity(String.class));
        }
    }

    @Test
    void uuidInvalidoDevuelve400SinConsultarElServicio() {
        try (Response response = target("clinicas/no-es-uuid").request().get()) {
            assertError(response, 400);
            verify(services.get(entityType(ClinicaResource.class)), never()).buscarPorId(any());
        }
        try (Response response = target("clinicas/1-1-1-1-1").request().get()) {
            assertError(response, 400);
        }
    }

    @Test
    void identificadorDelCuerpoDebeCoincidirConLaUrl() {
        try (Response response = target("clinicas/" + ID).request().put(Entity.json(
                "{\"idClinica\":\"" + MISSING + "\",\"nombre\":\"Clínica\"}"))) {
            assertError(response, 400);
            verify(services.get(entityType(ClinicaResource.class)), never()).actualizar(any());
        }
    }

    @Test
    void servidorGeneraElIdentificadorAlCrear() {
        try (Response response = target("clinicas").request().post(Entity.json(
                "{\"idClinica\":\"" + ID + "\",\"nombre\":\"Clínica\"}"))) {
            assertError(response, 400);
            verify(services.get(entityType(ClinicaResource.class)), never()).crear(any());
        }
    }

    @Test
    void relacionInexistenteDevuelve404AntesDePersistir() {
        try (Response response = target("personas-roles").request().post(Entity.json(
                "{\"idPersona\":\"" + MISSING + "\",\"idClinica\":\"" + ID + "\",\"idRol\":\"" + ID + "\"}"))) {
            assertError(response, 404);
            verify(services.get(entityType(PersonaRolResource.class)), never()).crear(any());
        }
    }

    @Test
    void cuerpoNuloJsonRotoYTiposIncorrectosDevuelven400() {
        for (String body : List.of("null", "{", "{\"nombre\":{}}", "{\"idClinica\":\"incorrecto\",\"nombre\":\"Clínica\"}")) {
            try (Response response = target("clinicas").request().post(Entity.json(body))) {
                assertError(response, 400);
            }
        }
        try (Response response = target("personas").request().post(Entity.json(
                "{\"nombres\":\"Ana\",\"apellidos\":\"Pérez\",\"fechaNacimiento\":\"fecha-invalida\"}"))) {
            assertError(response, 400);
        }
    }

    @Test
    void validaLongitudesYRangosDeFechas() {
        try (Response response = target("clinicas").request().post(Entity.json(
                "{\"nombre\":\"Clínica\",\"tipo\":\"" + "x".repeat(21) + "\"}"))) {
            assertError(response, 400);
        }
        try (Response response = target("consultas").request().post(Entity.json(
                "{\"idPersonaRol\":\"" + ID + "\",\"fechaInicio\":\"2020-01-02T00:00:00Z\",\"fechaFin\":\"2020-01-01T00:00:00Z\"}"))) {
            assertError(response, 400);
        }
    }

    @Test
    void rutasMetodosYFormatosIncorrectosTienenErroresJson() {
        try (Response response = target("ruta-inexistente").request().get()) {
            assertError(response, 404);
        }
        try (Response response = target("clinicas").request().method("PATCH", Entity.json("{}"))) {
            assertError(response, 405);
            assertTrue(response.getAllowedMethods().contains("GET"));
        }
        try (Response response = target("clinicas").request(MediaType.TEXT_PLAIN).get()) {
            assertError(response, 406);
        }
        try (Response response = target("clinicas").request().post(Entity.text("nombre"))) {
            assertError(response, 415);
        }
    }

    @Test
    void conservaAutenticacionYPermisosDelRuntime() {
        try (Response response = target("errores/401").request().get()) {
            assertError(response, 401);
            assertEquals("Bearer", response.getHeaderString("WWW-Authenticate"));
        }
        try (Response response = target("errores/403").request().get()) {
            assertError(response, 403);
        }
    }

    @Test
    void conflictosDeNegocioDevuelven409() {
        doThrow(new ServiceException("error.duplicado", "El registro ya existe.", null))
                .when(services.get(entityType(ClinicaResource.class))).crear(any());
        try (Response response = target("clinicas").request().post(Entity.json("{\"nombre\":\"Clínica\"}"))) {
            assertError(response, 409);
        }
    }

    @Test
    void erroresDePersistenciaDevuelven500SinExponerLaCausa() {
        doThrow(new ServiceException("error.crear", "SQL con información privada", new SQLException("dato privado", "08006")))
                .when(services.get(entityType(ClinicaResource.class))).crear(any());
        try (Response response = target("clinicas").request().post(Entity.json("{\"nombre\":\"Clínica\"}"))) {
            JsonObject error = assertError(response, 500);
            assertFalse(error.toString().contains("privad"));
            assertFalse(error.toString().contains("SQL"));
        }
    }

    @Test
    void erroresInesperadosDevuelven500() {
        when(services.get(entityType(ClinicaResource.class)).listarTodos()).thenThrow(new IllegalStateException("dato privado"));
        try (Response response = target("clinicas").request().get()) {
            JsonObject error = assertError(response, 500);
            assertFalse(error.toString().contains("privado"));
        }
    }

    static Stream<Arguments> erroresDeServicios() {
        return Stream.of(
                Arguments.of(new ServiceException("persona.nombreInvalido", "Nombre inválido.", null), 400),
                Arguments.of(new ServiceException("error.noExisteActualizar", "No existe.", null), 404),
                Arguments.of(new ServiceException("tipoDocumento.enUso", "Tiene documentos asociados.", null), 409),
                Arguments.of(new EJBException(new ServiceException("error.noExisteEliminar", "No existe.", null)), 404),
                Arguments.of(new EJBAccessException(), 403),
                Arguments.of(new EntityExistsException("detalle interno"), 409),
                Arguments.of(new OptimisticLockException("detalle interno"), 409),
                Arguments.of(new PersistenceException(new SQLException("detalle interno", "23505")), 409),
                Arguments.of(new PersistenceException(new SQLException("detalle interno", "23503")), 409),
                Arguments.of(new PersistenceException(new SQLException("detalle interno", "23502")), 400),
                Arguments.of(new PersistenceException(new SQLException("detalle interno", "23514")), 400),
                Arguments.of(new ServiceException("error.relacionado", "detalle interno",
                        new SQLException("detalle interno", "08006")), 500));
    }

    @ParameterizedTest
    @MethodSource("erroresDeServicios")
    void traduceErroresDeNegocioEjbYPersistencia(RuntimeException error, int status) {
        when(services.get(entityType(ClinicaResource.class)).listarTodos()).thenThrow(error);
        try (Response response = target("clinicas").request().get()) {
            assertError(response, status);
        }
    }

    @Test
    void validacionDeSalidaYFalloDelMotorSon500() {
        for (String path : List.of("errores/retorno-invalido", "errores/validacion")) {
            try (Response response = target(path).request().get()) {
                JsonObject error = assertError(response, 500);
                assertTrue(error.getJsonArray("details").isEmpty());
            }
        }
    }

    @Test
    void pingDevuelveJson() {
        try (Response response = target("jakartaee11").request().get()) {
            assertEquals(200, response.getStatus());
            assertEquals("ping Jakarta EE", json(response).getString("message"));
        }
    }

    private static JsonObject assertError(Response response, int status) {
        assertEquals(status, response.getStatus(), () -> response.readEntity(String.class));
        assertTrue(response.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE));
        JsonObject error = json(response);
        assertEquals(status, error.getInt("status"));
        assertFalse(error.getString("code").isBlank());
        assertFalse(error.getString("message").isBlank());
        assertTrue(error.getString("path").startsWith("/api/"));
        assertTrue(error.containsKey("timestamp"));
        return error;
    }

    private static JsonObject json(Response response) {
        return Json.createReader(new StringReader(response.readEntity(String.class))).readObject();
    }

    private static String route(Class<?> resource) {
        return resource.getAnnotation(Path.class).value();
    }

    private static Class<?> entityType(Class<?> resource) {
        return (Class<?>) ((ParameterizedType) resource.getGenericSuperclass()).getActualTypeArguments()[0];
    }

    private static Field idField(Class<?> entity) {
        return Stream.of(entity.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Id.class)).findFirst().orElseThrow();
    }

    private static UUID identifier(Object entity) throws ReflectiveOperationException {
        Field id = idField(entity.getClass());
        return (UUID) entity.getClass().getMethod("get" + capitalize(id.getName())).invoke(entity);
    }

    private static Object fixture(Class<?> entity) throws ReflectiveOperationException {
        Object value = entity.getConstructor(UUID.class).newInstance(ID);
        for (Field field : entity.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) continue;
            Class<?> type = field.getType();
            Object data;
            if (type == String.class) data = "Texto";
            else if (type == Boolean.class) data = true;
            else if (type == Date.class) data = new Date(946728000000L);
            else if (type == OffsetDateTime.class) data = OffsetDateTime.parse("2000-01-01T12:00:00Z");
            else if (type.getPackageName().equals(BASE_PACKAGE + ".entity")) data = type.getConstructor(UUID.class).newInstance(ID);
            else continue;
            entity.getMethod("set" + capitalize(field.getName()), type).invoke(value, data);
        }
        return value;
    }

    private static JsonObject payload(Class<?> entity) {
        JsonObjectBuilder body = Json.createObjectBuilder();
        for (Field field : entity.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) continue;
            Class<?> type = field.getType();
            if (type == String.class) body.add(field.getName(), "Texto");
            else if (type == Boolean.class) body.add(field.getName(), true);
            else if (type == Date.class) body.add(field.getName(), "2000-01-01T12:00:00.000Z");
            else if (type == OffsetDateTime.class) body.add(field.getName(), "2000-01-01T12:00:00Z");
            else if (type.getPackageName().equals(BASE_PACKAGE + ".entity")) body.add(field.getName(), ID.toString());
        }
        return body.build();
    }

    private static JsonObject updatedPayload(Class<?> entity) {
        JsonObjectBuilder body = Json.createObjectBuilder(payload(entity));
        for (Field field : entity.getDeclaredFields()) {
            if (field.getType() == String.class) body.add(field.getName(), "Actualizado");
            else if (field.getType() == Boolean.class) body.add(field.getName(), false);
            else if (field.getType() == Date.class) body.add(field.getName(), "2000-01-02T12:00:00.000Z");
            else if (field.getType() == OffsetDateTime.class) body.add(field.getName(), "2000-01-02T12:00:00Z");
        }
        return body.build();
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    @Path("errores")
    @Produces(MediaType.APPLICATION_JSON)
    public static class ErrorsResource {
        @GET
        @Path("401")
        public Response noAutenticado() {
            throw new NotAuthorizedException("Bearer");
        }

        @GET
        @Path("403")
        public Response sinPermisos() {
            throw new ForbiddenException();
        }

        @GET
        @Path("retorno-invalido")
        @NotNull
        public String retornoInvalido() {
            return null;
        }

        @GET
        @Path("validacion")
        public Response falloValidacion() {
            throw new ValidationException("detalle interno");
        }
    }
}
