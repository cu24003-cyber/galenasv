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
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.AppConfig;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.ApiExceptionMapper;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.JsonBodyReaderInterceptor;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.ValidationExceptionMapper;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.WebApplicationExceptionMapper;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AbstractService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.RegistroPersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConfiguracionProcedimientoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Consulta;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimiento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimientoPaso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.OrdenExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

/** Prueba el contrato HTTP con Jakarta REST y JSON-B reales, sin tocar la base de datos. */
class RestApiTest extends JerseyTest {
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    @AfterAll static void cerrarValidadores() { VALIDATORS.close(); }
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
    private AtencionService atencion;
    private AtencionSesion sesion;
    private ConfiguracionProcedimientoService configuracion;
    private RegistroPersonaService registro;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Application configure() {
        services = new HashMap<>();
        records = new HashMap<>();
        ResourceConfig config = new ResourceConfig().property(ServerProperties.WADL_FEATURE_DISABLE, true);
        Map<Class, Object> bindings = new HashMap<>();
        atencion = mock(AtencionService.class);
        sesion = mock(AtencionSesion.class);
        configuracion = mock(ConfiguracionProcedimientoService.class);
        registro = mock(RegistroPersonaService.class);
        bindings.put(Validator.class, VALIDATORS.getValidator());
        bindings.put(AtencionService.class, atencion);
        bindings.put(AtencionSesion.class, sesion);
        bindings.put(ConfiguracionProcedimientoService.class, configuracion);
        bindings.put(RegistroPersonaService.class, registro);
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
                when(service.listarPorFiltros(anyMap())).thenAnswer(i -> List.copyOf(data.values()));
                when(service.buscarPorId(any(UUID.class))).thenAnswer(i -> data.get(i.getArgument(0)));
                when(service.perteneceAFiltros(any(UUID.class), anyMap())).thenReturn(true);
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
        Consulta consulta = (Consulta) records.get(Consulta.class).get(ID);
        ConsultaProcedimiento procedimiento = (ConsultaProcedimiento) records.get(ConsultaProcedimiento.class).get(ID);
        procedimiento.setIdConsulta(consulta);
        ConsultaProcedimientoPaso paso = (ConsultaProcedimientoPaso) records.get(ConsultaProcedimientoPaso.class).get(ID);
        paso.setIdConsultaProcedimiento(procedimiento);
        ((OrdenExamen) records.get(OrdenExamen.class).get(ID)).setIdConsultaProcedimientoPaso(paso);
        ((sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento) records.get(entityType(TipoDocumentoResource.class)).get(ID)).setExpresionRegular(".");
        ((sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoMedioContacto) records.get(entityType(TipoMedioContactoResource.class)).get(ID)).setExpresionRegular(".");
        when(sesion.getRolActivo()).thenReturn((PersonaRol) records.get(PersonaRol.class).get(ID));
        AccesoSesion acceso = mock(AccesoSesion.class);
        AutenticacionService autenticacion = mock(AutenticacionService.class);
        when(acceso.getIdUsuario()).thenReturn(ID);
        when(autenticacion.contexto(eq(ID), any())).thenAnswer(i ->
                new AutenticacionService.Contexto(null, (PersonaRol) records.get(PersonaRol.class).get(i.getArgument(1))));
        bindings.put(AccesoSesion.class, acceso);
        bindings.put(AutenticacionService.class, autenticacion);
        bindings.put(HttpServletRequest.class, mock(HttpServletRequest.class));
        var documentos = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.DocumentoService) bindings.get(
                sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.DocumentoService.class);
        doAnswer(i -> { records.get(Documento.class).remove(i.getArgument(0)); return null; })
                .when(documentos).eliminarDePersona(any(), any());
        var contactos = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.MedioContactoService) bindings.get(
                sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.MedioContactoService.class);
        doAnswer(i -> { records.get(entityType(MedioContactoResource.class)).remove(i.getArgument(0)); return null; })
                .when(contactos).eliminarDePersona(any(), any());
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindings.forEach((type, service) -> bind(service).to(type));
            }
        });
        return config.register(JakartaEE11Resource.class).register(AtencionResource.class).register(ErrorsResource.class)
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

    static Stream<Class<?>> recursosCrud() {
        return recursos();
    }

    static Stream<Class<?>> recursosCreables() {
        return recursos();
    }

    @ParameterizedTest(name = "CRUD JSON: {0}")
    @MethodSource("recursosCrud")
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
    @MethodSource("recursosCreables")
    void rechazaDatosIncompletosAntesDePersistir(Class<?> resource) {
        try (Response response = target(route(resource)).request().post(Entity.json("{}"))) {
            JsonObject error = assertError(response, 400);
            assertFalse(error.getJsonArray("details").isEmpty());
            verify(services.get(entityType(resource)), never()).crear(any());
        }
    }

    @ParameterizedTest(name = "404: {0}")
    @MethodSource("recursosCrud")
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

    static Stream<Arguments> filtrosRelacionados() {
        return Stream.of(
                Arguments.of(PersonaResource.class, "idClinica", "personaRolCollection.idClinica.idClinica"),
                Arguments.of(PersonaResource.class, "idRol", "personaRolCollection.idRol.idRol"),
                Arguments.of(DocumentoResource.class, "idPersona", "idPersona.idPersona"),
                Arguments.of(MedioContactoResource.class, "idPersona", "idPersona.idPersona"),
                Arguments.of(PersonaRolResource.class, "idClinica", "idClinica.idClinica"),
                Arguments.of(ConsultaResource.class, "idPersona", "idPersonaRol.idPersona.idPersona"),
                Arguments.of(ConsultaResource.class, "idClinica", "idPersonaRol.idClinica.idClinica"),
                Arguments.of(ConsultaProcedimientoResource.class, "idConsulta", "idConsulta.idConsulta"),
                Arguments.of(ConsultaProcedimientoPasoResource.class, "idConsultaProcedimiento", "idConsultaProcedimiento.idConsultaProcedimiento"),
                Arguments.of(OrdenExamenResource.class, "idConsultaProcedimientoPaso", "idConsultaProcedimientoPaso.idConsultaProcedimientoPaso"),
                Arguments.of(ExamenResultadoResource.class, "idOrdenExamen", "idOrdenExamen.idOrdenExamen"),
                Arguments.of(ExamenTipoExamenResource.class, "idExamen", "idExamen.idExamen"),
                Arguments.of(ExamenResource.class, "idTipoExamen", "examenTipoExamenCollection.idTipoExamen.idTipoExamen"),
                Arguments.of(ProcedimientoPasoResource.class, "idProcedimiento", "idProcedimiento.idProcedimiento"),
                Arguments.of(ProcedimientoPasoExamenResource.class, "idProcedimientoPaso", "idProcedimientoPaso.idProcedimientoPaso"),
                Arguments.of(ProcedimientoPasoSecuenciaResource.class, "idProcedimientoPasoReferencia", "idProcedimientoPasoReferencia.idProcedimientoPaso"));
    }

    @ParameterizedTest
    @MethodSource("filtrosRelacionados")
    void filtraEnElServicioYConservaElContratoDto(Class<?> resource, String filtro, String atributo) {
        try (Response response = target(route(resource)).queryParam(filtro, ID).request().get()) {
            assertEquals(200, response.getStatus());
            var datos = Json.createReader(new StringReader(response.readEntity(String.class))).readArray();
            assertEquals(1, datos.size());
            assertEquals(ID.toString(), datos.getJsonObject(0).getString(idField(entityType(resource)).getName()));
            assertFalse(datos.getJsonObject(0).keySet().stream().anyMatch(k -> k.endsWith("Collection")));
        }
        verify(services.get(entityType(resource))).listarPorFiltros(Map.of(atributo, ID));
        verify(services.get(entityType(resource)), never()).listarTodos();
    }

    @Test
    void combinaClinicaYRolParaBuscarPersonas() {
        try (Response response = target("personas").queryParam("idClinica", ID)
                .queryParam("idRol", MISSING).request().get()) {
            assertEquals(200, response.getStatus());
        }
        verify(services.get(entityType(PersonaResource.class))).listarPorFiltros(Map.of(
                "personaRolCollection.idClinica.idClinica", ID,
                "personaRolCollection.idRol.idRol", MISSING));
    }

    @Test
    void documentosDeUnaPersonaNoIncluyenDocumentosDeOtra() throws Exception {
        var service = services.get(entityType(DocumentoResource.class));
        Documento otro = (Documento) fixture(Documento.class);
        otro.setIdDocumento(MISSING);
        otro.setIdPersona(new Persona(MISSING));
        records.get(entityType(DocumentoResource.class)).put(MISSING, otro);
        when(service.listarPorFiltros(Map.of("idPersona.idPersona", ID)))
                .thenReturn(List.of(records.get(entityType(DocumentoResource.class)).get(ID)));
        try (Response response = target("personas/" + ID + "/documentos").request().get()) {
            assertEquals(200, response.getStatus());
            var datos = Json.createReader(new StringReader(response.readEntity(String.class))).readArray();
            assertEquals(1, datos.size());
            assertEquals(ID.toString(), datos.getJsonObject(0).getString("idDocumento"));
        }
        verify(service, never()).listarTodos();
    }

    static Stream<Arguments> coleccionesRelacionadas() {
        return Stream.of(
                Arguments.of("clinicas", "personas", PersonaResource.class, "personaRolCollection.idClinica.idClinica"),
                Arguments.of("clinicas", "personas-roles", PersonaRolResource.class, "idClinica.idClinica"),
                Arguments.of("clinicas", "consultas", ConsultaResource.class, "idPersonaRol.idClinica.idClinica"),
                Arguments.of("personas", "documentos", DocumentoResource.class, "idPersona.idPersona"),
                Arguments.of("personas", "medios-contacto", MedioContactoResource.class, "idPersona.idPersona"),
                Arguments.of("personas", "personas-roles", PersonaRolResource.class, "idPersona.idPersona"),
                Arguments.of("personas", "consultas", ConsultaResource.class, "idPersonaRol.idPersona.idPersona"),
                Arguments.of("personas-roles", "consultas", ConsultaResource.class, "idPersonaRol.idPersonaRol"),
                Arguments.of("personas-roles", "pasos-consulta", ConsultaProcedimientoPasoResource.class, "idPersonaRol.idPersonaRol"),
                Arguments.of("consultas", "procedimientos", ConsultaProcedimientoResource.class, "idConsulta.idConsulta"),
                Arguments.of("consultas-procedimientos", "pasos", ConsultaProcedimientoPasoResource.class, "idConsultaProcedimiento.idConsultaProcedimiento"),
                Arguments.of("consultas-procedimientos-pasos", "ordenes-examen", OrdenExamenResource.class, "idConsultaProcedimientoPaso.idConsultaProcedimientoPaso"),
                Arguments.of("ordenes-examen", "resultados", ExamenResultadoResource.class, "idOrdenExamen.idOrdenExamen"),
                Arguments.of("procedimientos", "pasos", ProcedimientoPasoResource.class, "idProcedimiento.idProcedimiento"),
                Arguments.of("procedimientos", "consultas-procedimientos", ConsultaProcedimientoResource.class, "idProcedimiento.idProcedimiento"),
                Arguments.of("procedimientos-pasos", "examenes-configurados", ProcedimientoPasoExamenResource.class, "idProcedimientoPaso.idProcedimientoPaso"),
                Arguments.of("procedimientos-pasos", "secuencias", ProcedimientoPasoSecuenciaResource.class, "idProcedimientoPaso.idProcedimientoPaso"),
                Arguments.of("procedimientos-pasos", "pasos-consulta", ConsultaProcedimientoPasoResource.class, "idProcedimientoPaso.idProcedimientoPaso"),
                Arguments.of("roles", "personas", PersonaResource.class, "personaRolCollection.idRol.idRol"),
                Arguments.of("roles", "personas-roles", PersonaRolResource.class, "idRol.idRol"),
                Arguments.of("roles", "procedimientos-pasos", ProcedimientoPasoResource.class, "idRol.idRol"),
                Arguments.of("tipos-documento", "documentos", DocumentoResource.class, "idTipoDocumento.idTipoDocumento"),
                Arguments.of("tipos-medio-contacto", "medios-contacto", MedioContactoResource.class, "idTipoMedioContacto.idTipoMedioContacto"),
                Arguments.of("tipos-examen", "examenes", ExamenResource.class, "examenTipoExamenCollection.idTipoExamen.idTipoExamen"),
                Arguments.of("tipos-examen", "clasificaciones", ExamenTipoExamenResource.class, "idTipoExamen.idTipoExamen"),
                Arguments.of("examenes", "clasificaciones", ExamenTipoExamenResource.class, "idExamen.idExamen"));
    }

    @ParameterizedTest
    @MethodSource("coleccionesRelacionadas")
    void rutaAnidadaFiltraPorElPadre(String padre, String coleccion, Class<?> resource, String atributo) {
        try (Response response = target(padre + "/" + ID + "/" + coleccion).request().get()) {
            assertEquals(200, response.getStatus());
            var datos = Json.createReader(new StringReader(response.readEntity(String.class))).readArray();
            assertEquals(1, datos.size());
            assertTrue(datos.getJsonObject(0).containsKey(idField(entityType(resource)).getName()));
        }
        verify(services.get(entityType(resource))).listarPorFiltros(Map.of(atributo, ID));
        verify(services.get(entityType(resource)), never()).listarTodos();
    }

    @Test
    void permiteAfinarUnaColeccionSinCambiarElPadre() {
        try (Response response = target("personas/" + ID + "/documentos")
                .queryParam("idTipoDocumento", MISSING).queryParam("idPersona", ID).request().get()) {
            assertEquals(200, response.getStatus());
        }
        verify(services.get(entityType(DocumentoResource.class))).listarPorFiltros(Map.of(
                "idPersona.idPersona", ID, "idTipoDocumento.idTipoDocumento", MISSING));
    }

    @Test
    void filtrosSinCoincidenciasDevuelvenArregloVacioInclusoSiLaRelacionNoExiste() {
        var service = services.get(entityType(DocumentoResource.class));
        when(service.listarPorFiltros(anyMap())).thenReturn(List.of());
        for (String path : List.of("documentos", "personas/" + ID + "/documentos")) {
            try (Response response = target(path).queryParam("idTipoDocumento", MISSING).request().get()) {
                assertEquals(200, response.getStatus());
                assertEquals("[]", response.readEntity(String.class));
            }
        }
    }

    @Test
    void padreInexistenteUuidInvalidoOColeccionDesconocidaNoConsultaLosHijos() {
        for (String path : List.of("personas/" + MISSING + "/documentos", "personas/" + ID + "/desconocida")) {
            try (Response response = target(path).request().get()) {
                assertError(response, 404);
            }
        }
        try (Response response = target("personas/no-es-uuid/documentos").request().get()) {
            assertError(response, 400);
        }
        verify(services.get(entityType(DocumentoResource.class)), never()).listarPorFiltros(anyMap());
    }

    @Test
    void filtrosInvalidosRepetidosDesconocidosOContradictoriosDevuelven400() {
        for (String valor : List.of("no-es-uuid", "1-1-1-1-1", "")) {
            try (Response response = target("documentos").queryParam("idPersona", valor).request().get()) {
                assertEquals("id.invalido", assertError(response, 400).getString("code"));
            }
        }
        try (Response response = target("documentos").queryParam("idPersona", ID, MISSING).request().get()) {
            assertEquals("filtro.repetido", assertError(response, 400).getString("code"));
        }
        try (Response response = target("documentos").queryParam("idPersona.idPersona", ID).request().get()) {
            assertEquals("filtro.desconocido", assertError(response, 400).getString("code"));
        }
        try (Response response = target("personas/" + ID + "/documentos")
                .queryParam("idPersona", MISSING).request().get()) {
            assertEquals("filtro.inconsistente", assertError(response, 400).getString("code"));
        }
        verify(services.get(entityType(DocumentoResource.class)), never()).listarPorFiltros(anyMap());
        verify(services.get(entityType(DocumentoResource.class)), never()).listarTodos();
    }

    @Test
    void filtrosDeclaradosApuntanAIdentificadoresPersistidosYLasColeccionesLosAdmiten() throws Exception {
        for (Class<?> type : RESOURCES) {
            AbstractResource<?, ?> resource = (AbstractResource<?, ?>) type.getConstructor().newInstance();
            for (String path : resource.filtrosPermitidos().values()) {
                Class<?> attributeType = entityType(type);
                Field attribute = null;
                for (String name : path.split("\\.")) {
                    attribute = attributeType.getDeclaredField(name);
                    attributeType = java.util.Collection.class.isAssignableFrom(attribute.getType())
                            ? (Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]
                            : attribute.getType();
                }
                assertEquals(UUID.class, attributeType, type.getSimpleName() + ": " + path);
                assertTrue(attribute.isAnnotationPresent(Id.class), path);
            }
            for (var collection : resource.coleccionesRelacionadas().values()) {
                AbstractResource<?, ?> child = collection.recurso().getConstructor().newInstance();
                assertTrue(child.filtrosPermitidos().containsKey(collection.filtro()));
            }
        }
    }

    static Stream<Arguments> coleccionesEditables() {
        return Stream.of(
                Arguments.of("personas", "documentos", DocumentoResource.class, "idPersona"),
                Arguments.of("personas", "medios-contacto", MedioContactoResource.class, "idPersona"),
                Arguments.of("personas", "personas-roles", PersonaRolResource.class, "idPersona"),
                Arguments.of("clinicas", "personas-roles", PersonaRolResource.class, "idClinica"),
                Arguments.of("roles", "personas-roles", PersonaRolResource.class, "idRol"),
                Arguments.of("examenes", "clasificaciones", ExamenTipoExamenResource.class, "idExamen"),
                Arguments.of("tipos-examen", "clasificaciones", ExamenTipoExamenResource.class, "idTipoExamen"));
    }

    @ParameterizedTest
    @MethodSource("coleccionesEditables")
    void crudAnidadoInfiereElPadreYOperaSobreElHijo(String padre, String coleccion, Class<?> resource, String campo) {
        String base = padre + "/" + ID + "/" + coleccion;
        var body = Json.createObjectBuilder(payload(entityType(resource))).remove(campo).build();
        UUID nuevo;
        try (Response response = target(base).request().post(Entity.json(body.toString()))) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
            JsonObject datos = json(response);
            nuevo = UUID.fromString(datos.getString(idField(entityType(resource)).getName()));
            assertEquals(ID.toString(), datos.getString(campo));
            assertEquals(getBaseUri().resolve(base + "/" + nuevo), response.getLocation());
        }
        String path = base + "/" + nuevo;
        try (Response response = target(path).request().get()) {
            assertEquals(200, response.getStatus());
            assertEquals(nuevo.toString(), json(response).getString(idField(entityType(resource)).getName()));
        }
        try (Response response = target(path).request().put(Entity.json(body.toString()))) {
            assertEquals(200, response.getStatus());
        }
        verify(services.get(entityType(resource))).actualizar(any());
        try (Response response = target(path).request().delete()) {
            assertEquals(204, response.getStatus());
        }
        try (Response response = target(route(resource) + "/" + nuevo).request().get()) {
            assertError(response, 404);
        }
    }

    @Test
    void patchModificaSoloLosCamposEnviadosYPermiteBorrarUnOpcional() {
        String path = "personas/" + ID + "/documentos/" + ID;
        try (Response response = target(path).request().method("PATCH", Entity.entity(
                "{\"valor\":\"nuevo\",\"rutaFisica\":null}", "application/merge-patch+json"))) {
            assertEquals(200, response.getStatus(), () -> response.readEntity(String.class));
            JsonObject datos = json(response);
            assertEquals("nuevo", datos.getString("valor"));
            assertEquals(ID.toString(), datos.getString("idTipoDocumento"));
            assertEquals(ID.toString(), datos.getString("idPersona"));
            assertFalse(datos.containsKey("rutaFisica"));
        }
        verify(services.get(Documento.class)).actualizar(any());
    }

    @Test
    void patchInvalidoNoModificaLaEntidad() {
        for (String body : List.of("{", "[]", "null", "{\"valor\":null}", "{\"idDocumento\":null}",
                "{\"desconocido\":true}", "{\"idPersona\":\"" + MISSING + "\"}")) {
            try (Response response = target("personas/" + ID + "/documentos/" + ID).request()
                    .method("PATCH", Entity.json(body))) {
                assertError(response, 400);
            }
        }
        assertEquals("Texto", ((Documento) records.get(Documento.class).get(ID)).getValor());
        verify(services.get(Documento.class), never()).actualizar(any());
    }

    @Test
    void noPermiteMoverUnHijoAMedianteElCuerpoNiOperarSobreOtroPadre() {
        var service = services.get(Documento.class);
        String base = "personas/" + ID + "/documentos";
        String otroPadre = Json.createObjectBuilder(payload(Documento.class)).add("idPersona", MISSING.toString()).build().toString();
        try (Response response = target(base).request().post(Entity.json(otroPadre))) { assertError(response, 400); }
        try (Response response = target(base + "/" + ID).request().put(Entity.json(otroPadre))) { assertError(response, 400); }
        when(service.perteneceAFiltros(eq(ID), anyMap())).thenReturn(false);
        try (Response response = target(base + "/" + ID).request().get()) { assertError(response, 404); }
        try (Response response = target(base + "/" + ID).request().put(Entity.json(payload(Documento.class).toString()))) { assertError(response, 404); }
        try (Response response = target(base + "/" + ID).request().method("PATCH", Entity.json("{\"valor\":\"cambio\"}"))) { assertError(response, 404); }
        try (Response response = target(base + "/" + ID).request().delete()) { assertError(response, 404); }
        verify(service, never()).crear(any());
        verify(service, never()).actualizar(any());
        verify((sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.DocumentoService) (Object) service,
                never()).eliminarDePersona(any(), any());
    }

    @Test
    void headYOpcionesAnidadasReflejanLosMetodosAdmitidos() {
        try (Response response = target("personas/" + ID + "/documentos").request().head()) {
            assertEquals(200, response.getStatus());
            assertFalse(response.hasEntity());
        }
        try (Response response = target("personas/" + ID + "/documentos").request().options()) {
            assertEquals(java.util.Set.of("GET", "HEAD", "OPTIONS", "POST"), response.getAllowedMethods());
        }
        try (Response response = target("personas/" + ID + "/documentos/" + ID).request().options()) {
            assertEquals(java.util.Set.of("GET", "HEAD", "OPTIONS", "PUT", "PATCH", "DELETE"), response.getAllowedMethods());
        }
        try (Response response = target("clinicas/" + ID + "/personas/" + ID).request().delete()) {
            assertError(response, 405);
            assertFalse(response.getAllowedMethods().contains("DELETE"));
        }
    }

    @Test
    void nuevasRutasClinicasRestringenLasEscriturasAlFlujo() {
        for (String route : List.of("personas-roles/" + ID + "/consultas",
                "consultas/" + ID + "/procedimientos", "consultas-procedimientos/" + ID + "/pasos",
                "consultas-procedimientos-pasos/" + ID + "/ordenes-examen", "ordenes-examen/" + ID + "/resultados")) {
            try (Response response = target(route + "/" + ID).request().delete()) { assertError(response, 405); }
        }
        try (Response response = target("consultas-procedimientos/" + ID + "/pasos").request().post(Entity.json("{}"))) { assertError(response, 405); }
        try (Response response = target("consultas-procedimientos/" + ID).request().method("PATCH", Entity.json("{}"))) { assertError(response, 405); }
        for (Class<?> resource : List.of(ConsultaResource.class, ConsultaProcedimientoResource.class,
                ConsultaProcedimientoPasoResource.class, OrdenExamenResource.class, ExamenResultadoResource.class)) {
            verify(services.get(entityType(resource)), never()).eliminar(any());
        }
    }

    @Test
    void creaPersonaEnClinicaConElRegistroAtomicoDePertenencia() {
        doAnswer(i -> {
            Persona persona = i.getArgument(0);
            persona.setIdPersona(MISSING);
            records.get(Persona.class).put(MISSING, persona);
            return UUID.randomUUID();
        }).when(registro).guardar(any(), isNull(), any(), isNull());
        try (Response response = target("clinicas/" + ID + "/personas").request().post(Entity.json(payload(Persona.class).toString()))) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
            assertEquals(MISSING.toString(), json(response).getString("idPersona"));
        }
        verify(registro).guardar(any(), isNull(), any(), isNull());
        verify(services.get(Persona.class), never()).crear(any());
    }

    @Test
    void creaConsultaDesdeElPacienteConElServicioDeAtencionYLaSesion() {
        Consulta nueva = new Consulta(MISSING);
        nueva.setIdPersonaRol((PersonaRol) records.get(PersonaRol.class).get(ID));
        when(atencion.crear(ID, "REF", "notas")).thenReturn(nueva);
        try (Response response = target("personas-roles/" + ID + "/consultas").request()
                .post(Entity.json("{\"referenciaExterna\":\"REF\",\"observaciones\":\"notas\"}"))) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
            assertEquals(MISSING.toString(), json(response).getString("idConsulta"));
        }
        verify(atencion).crear(ID, "REF", "notas");
        verify(sesion).setConsulta(MISSING);
        verify(services.get(Consulta.class), never()).crear(any());
    }

    @Test
    void actualizaNotasDeConsultaSinModificarFechasYUsaCierreParaFinalizar() {
        Consulta consulta = (Consulta) records.get(Consulta.class).get(ID);
        consulta.setFechaFin(null);
        when(sesion.getConsulta()).thenReturn(ID);
        try (Response response = target("consultas/" + ID).request()
                .method("PATCH", Entity.json("{\"observaciones\":\"notas\"}"))) {
            assertEquals(200, response.getStatus());
        }
        verify(atencion).guardar(ID, "Texto", "notas");
        try (Response response = target("consultas/" + ID).request()
                .method("PATCH", Entity.json("{\"fechaFin\":null}"))) { assertError(response, 400); }
        try (Response response = target("consultas/" + ID + "/cierre").request()
                .put(Entity.json("{\"referenciaExterna\":\"REF\",\"observaciones\":\"cierre\"}"))) {
            assertEquals(200, response.getStatus());
        }
        verify(atencion).cerrar(ID, "REF", "cierre");
        verify(sesion).setConsulta(null);
    }

    @Test
    void iniciarProcedimientoCrearOrdenYRegistrarResultadoDeleganEnElFlujo() {
        when(atencion.agregarProcedimiento(ID, ID, "notas", ID)).thenReturn((ConsultaProcedimiento) records.get(ConsultaProcedimiento.class).get(ID));
        try (Response response = target("consultas/" + ID + "/procedimientos").request()
                .post(Entity.json("{\"idProcedimiento\":\"" + ID + "\",\"observaciones\":\"notas\"}"))) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
        }
        when(atencion.ordenar(ID, ID, "Hemograma")).thenReturn((OrdenExamen) records.get(OrdenExamen.class).get(ID));
        try (Response response = target("consultas-procedimientos-pasos/" + ID + "/ordenes-examen").request()
                .post(Entity.json("{\"indicaciones\":\"Hemograma\"}"))) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
        }
        var resultado = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ExamenResultado) records.get(entityType(ExamenResultadoResource.class)).get(ID);
        when(atencion.registrarResultado(ID, ID, "Normal", "interpretación")).thenReturn(resultado);
        try (Response response = target("ordenes-examen/" + ID + "/resultados").request()
                .post(Entity.json("{\"resultado\":\"Normal\",\"interpretacion\":\"interpretación\"}"))) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
        }
        verify(atencion).agregarProcedimiento(ID, ID, "notas", ID);
        verify(atencion).ordenar(ID, ID, "Hemograma");
        verify(atencion).registrarResultado(ID, ID, "Normal", "interpretación");
        verify(services.get(ConsultaProcedimiento.class), never()).crear(any());
        verify(services.get(OrdenExamen.class), never()).crear(any());
        verify(services.get(entityType(ExamenResultadoResource.class)), never()).crear(any());
    }

    @Test
    void asignaCompletaYRegistraExamenDelPasoMedianteAcciones() {
        String base = "consultas-procedimientos-pasos/" + ID;
        try (Response response = target(base + "/responsable").request()
                .put(Entity.json("{\"idPersonaRol\":\"" + MISSING + "\"}"))) { assertEquals(200, response.getStatus()); }
        try (Response response = target(base + "/finalizacion").request()
                .put(Entity.json("{\"valor\":\"125/80\"}"))) { assertEquals(200, response.getStatus()); }
        var examen = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Examen) records.get(entityType(ExamenResource.class)).get(ID);
        when(atencion.examen(ID, ID, "Hemograma", null, ID)).thenReturn(examen);
        try (Response response = target(base + "/examenes").request()
                .post(Entity.json("{\"nombre\":\"Hemograma\",\"idTipoExamen\":\"" + ID + "\"}"))) {
            assertEquals(201, response.getStatus());
            assertEquals(getBaseUri().resolve("examenes/" + ID), response.getLocation());
        }
        verify(atencion).asignarResponsable(ID, ID, MISSING);
        verify(atencion).completar(ID, ID, "125/80");
        verify(atencion).examen(ID, ID, "Hemograma", null, ID);
    }

    @Test
    void configuraPasoConPredecesorYAsociaExamenActivo() {
        var paso = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPaso) records.get(entityType(ProcedimientoPasoResource.class)).get(ID);
        when(configuracion.paso(ID, "Final", true, ID, MISSING)).thenReturn(paso);
        try (Response response = target("procedimientos/" + ID + "/pasos").request().post(Entity.json(
                "{\"nombre\":\"Final\",\"indicaFin\":true,\"idRol\":\"" + ID + "\",\"idPasoPrevio\":\"" + MISSING + "\"}"))) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
        }
        var asociacion = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPasoExamen) records.get(entityType(ProcedimientoPasoExamenResource.class)).get(ID);
        when(configuracion.asociar(ID, ID, ID)).thenReturn(asociacion);
        try (Response response = target("procedimientos-pasos/" + ID + "/examenes-configurados").request().post(Entity.json(
                "{\"idExamen\":\"" + ID + "\"}"))) { assertEquals(201, response.getStatus()); }
        verify(configuracion).paso(ID, "Final", true, ID, MISSING);
        verify(configuracion).asociar(ID, ID, ID);
    }

    @Test
    void codigoDeAccesoDelRolNoSePuedeCambiarDesdeElCrud() {
        var rol = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol)
                records.get(entityType(RolResource.class)).get(ID);
        rol.setCodigo("DOCTOR");
        try (Response response = target("roles/" + ID).request().method("PATCH", Entity.json("{\"codigo\":\"ADMINISTRACION\"}"))) {
            assertError(response, 400);
        }
        try (Response response = target("roles/" + ID).request().put(Entity.json(
                "{\"nombre\":\"Doctor\",\"activo\":true,\"codigo\":\"ADMINISTRACION\"}"))) {
            assertEquals(200, response.getStatus());
        }
        assertEquals("DOCTOR", rol.getCodigo());
    }

    @Test
    void seleccionaRolActivoYRetomaConsultaAbierta() {
        try (Response response = target("atencion/rol-activo").request().put(Entity.json("{\"idPersonaRol\":\"" + ID + "\"}"))) {
            assertEquals(200, response.getStatus());
            assertEquals(ID.toString(), json(response).getString("idPersonaRol"));
        }
        verify(sesion).cambiarRol(any());
        ((Consulta) records.get(Consulta.class).get(ID)).setFechaFin(null);
        try (Response response = target("consultas/" + ID + "/sesion").request().put(Entity.json("{}"))) {
            assertEquals(200, response.getStatus());
        }
        verify(atencion).cargar(ID);
        verify(sesion).setConsulta(ID);
    }

    @Test
    void erroresDelFlujoNoPersistenPorCrudNiCambianLaSesion() {
        when(atencion.crear(ID, null, null)).thenThrow(new ServiceException(
                "consulta.pacienteEnCurso", "El paciente ya tiene una consulta en curso.", null));
        try (Response response = target("personas-roles/" + ID + "/consultas").request().post(Entity.json("{}"))) {
            assertEquals("consulta.pacienteEnCurso", assertError(response, 409).getString("code"));
        }
        doThrow(new ServiceException("consulta.yaCerrada", "La consulta ya está cerrada.", null))
                .when(atencion).guardar(ID, "Texto", "cambio");
        try (Response response = target("consultas/" + ID).request().method("PATCH", Entity.json("{\"observaciones\":\"cambio\"}"))) {
            assertEquals("consulta.yaCerrada", assertError(response, 409).getString("code"));
        }
        verify(services.get(Consulta.class), never()).crear(any());
        verify(services.get(Consulta.class), never()).actualizar(any());
        verify(sesion, never()).setConsulta(any());
    }

    @Test
    void pasoDeOtroProcedimientoNoSeCompletaDesdeLaRutaAnidada() {
        when(services.get(ConsultaProcedimientoPaso.class).perteneceAFiltros(eq(ID), anyMap())).thenReturn(false);
        try (Response response = target("consultas-procedimientos/" + ID + "/pasos/" + ID + "/finalizacion")
                .request().put(Entity.json("{\"valor\":\"125/80\"}"))) { assertError(response, 404); }
        verify(atencion, never()).completar(any(), any(), any());
    }

    @Test
    void valorDePasoConservaLaLongitudAdmitidaEnLaApp() {
        String valor = "x".repeat(600);
        String body = Json.createObjectBuilder().add("valor", valor).build().toString();
        try (Response response = target("consultas-procedimientos-pasos/" + ID + "/finalizacion")
                .request().put(Entity.json(body))) { assertEquals(200, response.getStatus()); }
        verify(atencion).completar(ID, ID, valor);
    }

    @Test
    void nuevasEscriturasDeDocumentoYContactoRespetanElFormatoYRecortanElValor() {
        var documento = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento) records.get(entityType(TipoDocumentoResource.class)).get(ID);
        var contacto = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoMedioContacto) records.get(entityType(TipoMedioContactoResource.class)).get(ID);
        documento.setExpresionRegular("\\d+");
        contacto.setExpresionRegular("\\d+");
        for (String coleccion : List.of("documentos", "medios-contacto")) {
            String campo = coleccion.equals("documentos") ? "idTipoDocumento" : "idTipoMedioContacto";
            String base = "personas/" + ID + "/" + coleccion;
            String body = Json.createObjectBuilder().add("valor", "  12345  ").add(campo, ID.toString()).build().toString();
            try (Response response = target(base).request().post(Entity.json(body))) {
                assertEquals(201, response.getStatus());
                assertEquals("12345", json(response).getString("valor"));
            }
            body = Json.createObjectBuilder().add("valor", "ABC").add(campo, ID.toString()).build().toString();
            try (Response response = target(base).request().post(Entity.json(body))) {
                assertEquals("registro.formatoInvalido", assertError(response, 400).getString("code"));
            }
        }
    }

    @Test
    void tiposInactivosNoSeAceptanEnLasNuevasEscrituras() {
        var tipo = (sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento) records.get(entityType(TipoDocumentoResource.class)).get(ID);
        tipo.setActivo(false);
        try (Response response = target("personas/" + ID + "/documentos").request().post(Entity.json(
                "{\"valor\":\"12345\",\"idTipoDocumento\":\"" + ID + "\"}"))) { assertError(response, 400); }
        try (Response response = target("documentos/" + ID).request().method("PATCH", Entity.json("{\"valor\":\"cambio\"}"))) { assertError(response, 400); }
        verify(services.get(Documento.class), never()).crear(any());
        verify(services.get(Documento.class), never()).actualizar(any());
    }

    @Test
    void asignacionConRolConservaSuRolCuandoSeOmiteComoEnLaFichaDePersonas() {
        String body = Json.createObjectBuilder().add("idClinica", ID.toString()).build().toString();
        try (Response response = target("personas/" + ID + "/personas-roles/" + ID).request().put(Entity.json(body))) {
            assertEquals(200, response.getStatus());
            assertEquals(ID.toString(), json(response).getString("idRol"));
        }
    }

    @Test
    void nuevasEscriturasDeAsignacionNoTrasladanElRegistroAOtraPersona() {
        String body = Json.createObjectBuilder().add("idPersona", MISSING.toString())
                .add("idRol", ID.toString()).build().toString();
        try (Response response = target("clinicas/" + ID + "/personas-roles/" + ID).request().put(Entity.json(body))) { assertError(response, 400); }
        try (Response response = target("personas-roles/" + ID).request().method("PATCH", Entity.json(body))) { assertError(response, 400); }
        verify(services.get(PersonaRol.class), never()).actualizar(any());
    }

    @Test
    void cierreSinNotasNuevasConservaLaReferenciaYLasObservaciones() {
        ((Consulta) records.get(Consulta.class).get(ID)).setFechaFin(null);
        try (Response response = target("consultas/" + ID + "/cierre").request().put(Entity.json("{}"))) {
            assertEquals(200, response.getStatus());
        }
        verify(atencion).cerrar(ID, "Texto", "Texto");
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
            if (entity.getSimpleName().equals("Rol") && field.getName().equals("codigo")) continue;
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
            if (entity.getSimpleName().equals("Rol") && field.getName().equals("codigo")) continue;
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
