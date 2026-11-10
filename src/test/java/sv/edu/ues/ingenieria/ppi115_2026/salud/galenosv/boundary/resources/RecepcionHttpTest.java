package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.persistence.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.*;
import java.util.*;
import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

class RecepcionHttpTest extends JerseyTest {
    static final ValidatorFactory VALIDADORES = Validation.buildDefaultValidatorFactory();
    RegistroPersonaService registro;
    Rol rolActivo;
    UUID personaId, asignacionId;
    @Override @SuppressWarnings("unchecked") protected Application configure() {
        var recurso = new RecepcionResource();
        registro = mock(RegistroPersonaService.class);
        var auth = mock(AutenticacionService.class);
        var acceso = mock(AccesoSesion.class);
        var atencion = mock(AtencionSesion.class);
        recurso.em = mock(EntityManager.class);
        rolActivo = new Rol(UUID.randomUUID()); rolActivo.setCodigo("ADMINISTRACION");
        var asignacion = new PersonaRol(UUID.randomUUID()); asignacion.setIdRol(rolActivo); asignacion.setIdClinica(new Clinica(UUID.randomUUID()));
        when(atencion.getRolActivo()).thenReturn(asignacion);
        when(auth.contexto(any(),any())).thenReturn(new AutenticacionService.Contexto(new Usuario(),asignacion));
        var rolPaciente = new Rol(UUID.randomUUID()); rolPaciente.setCodigo("PACIENTE");
        TypedQuery<Rol> query = mock(TypedQuery.class);
        when(recurso.em.createQuery(anyString(),eq(Rol.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(rolPaciente));
        personaId = UUID.randomUUID(); asignacionId = UUID.randomUUID();
        when(registro.guardar(any(),eq(rolPaciente),eq(asignacion.getIdClinica()),isNull())).thenAnswer(i -> {
            ((Persona)i.getArgument(0)).setIdPersona(personaId); return asignacionId;
        });
        return new ResourceConfig().register(recurso).register(ApiExceptionMapper.class)
                .register(ValidationExceptionMapper.class).register(JsonBodyReaderInterceptor.class)
                .register(new AbstractBinder() {
                    @Override protected void configure() {
                        bind(registro).to(RegistroPersonaService.class); bind(auth).to(AutenticacionService.class);
                        bind(acceso).to(AccesoSesion.class); bind(atencion).to(AtencionSesion.class);
                        bind(VALIDADORES.getValidator()).to(Validator.class);
                    }
                });
    }
    @Override protected TestContainerFactory getTestContainerFactory() { return new InMemoryTestContainerFactory(); }
    @BeforeEach void iniciar() throws Exception { super.setUp(); }
    @AfterEach void cerrar() throws Exception { super.tearDown(); }
    @AfterAll static void cerrarValidadores() { VALIDADORES.close(); }
    @Test void formularioDelFrontendDevuelvePersonaYAsignacionPorHttp() {
        try (Response respuesta = target("recepcion/pacientes").request().post(Entity.json(
                "{\"nombres\":\"Ana\",\"apellidos\":\"Pérez\",\"fechaNacimiento\":\"2000-01-01T00:00:00.000Z\"}"))) {
            assertEquals(201,respuesta.getStatus());
            var body = respuesta.readEntity(jakarta.json.JsonObject.class);
            assertEquals(personaId.toString(),body.getString("idPersona"));
            assertEquals(asignacionId.toString(),body.getString("idPersonaRol"));
            assertTrue(respuesta.getLocation().toString().endsWith("personas/"+personaId));
        }
    }
    @Test void datosInvalidosNoLleganAlRegistro() {
        for (String datos : List.of("{}", "{\"nombres\":\" \" ,\"apellidos\":\"Pérez\",\"fechaNacimiento\":\"2000-01-01T00:00:00.000Z\"}",
                "{\"nombres\":\"Ana\",\"apellidos\":\"Pérez\",\"fechaNacimiento\":\"3000-01-01T00:00:00.000Z\"}")) {
            try (Response respuesta = target("recepcion/pacientes").request().post(Entity.json(datos))) { assertEquals(400,respuesta.getStatus()); }
        }
        verifyNoInteractions(registro);
    }
    @Test void otroPerfilNoRegistraAunqueLlameDirectamenteAlRecurso() {
        rolActivo.setCodigo("DOCTOR");
        try (Response respuesta = target("recepcion/pacientes").request().post(Entity.json(
                "{\"nombres\":\"Ana\",\"apellidos\":\"Pérez\",\"fechaNacimiento\":\"2000-01-01T00:00:00.000Z\"}"))) { assertEquals(403,respuesta.getStatus()); }
        verifyNoInteractions(registro);
    }
    @Test void nombresYApellidosConNumerosNoLleganAlRegistro() {
        for (String campo : List.of("nombres", "apellidos")) {
            var body=jakarta.json.Json.createObjectBuilder().add("nombres", "María José").add("apellidos", "Peña")
                    .add("fechaNacimiento", "2000-01-01T00:00:00.000Z").add(campo,"Ana123").build();
            try(Response respuesta=target("recepcion/pacientes").request().post(Entity.json(body.toString()))) {
                assertEquals(400,respuesta.getStatus());
            }
        }
        verifyNoInteractions(registro);
    }
}
