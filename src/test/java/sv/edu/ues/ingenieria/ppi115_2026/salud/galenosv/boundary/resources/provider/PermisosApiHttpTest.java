package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
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
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

class PermisosApiHttpTest extends JerseyTest {
    PersonaRol activo, paciente, ajeno;
    Rol rol;
    EntityManager em;
    AutenticacionService auth;
    UUID clinica;
    @Path("/") public static class Echo {
        @GET @Path("{ruta:.*}") public String leer(@Context UriInfo uri) { return uri.getRequestUri().toString(); }
        @POST @Path("{ruta:.*}") @Consumes(MediaType.APPLICATION_JSON) public String crear(String json) { return json; }
        @PUT @Path("{ruta:.*}") @Consumes(MediaType.APPLICATION_JSON) public String editar(String json) { return json; }
    }
    @Override protected Application configure() {
        em=mock(EntityManager.class); auth=mock(AutenticacionService.class);
        var acceso=mock(AccesoSesion.class); var atencion=mock(AtencionSesion.class);
        clinica=UUID.randomUUID(); rol=new Rol(UUID.randomUUID()); rol.setCodigo("DOCTOR"); rol.setActivo(true);
        activo=new PersonaRol(UUID.randomUUID()); activo.setIdRol(rol); activo.setIdClinica(new Clinica(clinica)); activo.setIdPersona(new Persona(UUID.randomUUID()));
        paciente=new PersonaRol(UUID.randomUUID()); paciente.setIdClinica(new Clinica(clinica)); paciente.setIdPersona(new Persona(UUID.randomUUID()));
        ajeno=new PersonaRol(UUID.randomUUID()); ajeno.setIdClinica(new Clinica(UUID.randomUUID())); ajeno.setIdPersona(new Persona(UUID.randomUUID()));
        Usuario usuario=new Usuario(); usuario.setPersona(activo.getIdPersona());
        when(atencion.getRolActivo()).thenReturn(activo);
        when(auth.contexto(any(),any())).thenAnswer(i -> new AutenticacionService.Contexto(usuario, activo));
        when(em.find(PersonaRol.class,paciente.getIdPersonaRol())).thenReturn(paciente);
        when(em.find(PersonaRol.class,ajeno.getIdPersonaRol())).thenReturn(ajeno);
        when(em.find(PersonaRol.class,activo.getIdPersonaRol())).thenReturn(activo);
        var filtro=new PermisosApiFilter(); filtro.em=em;
        return new ResourceConfig(Echo.class, ApiExceptionMapper.class).register(filtro).register(new AbstractBinder() {
            @Override protected void configure() { bind(auth).to(AutenticacionService.class); bind(acceso).to(AccesoSesion.class); bind(atencion).to(AtencionSesion.class); }
        });
    }
    @Override protected TestContainerFactory getTestContainerFactory() { return new InMemoryTestContainerFactory(); }
    @BeforeEach void iniciar() throws Exception { super.setUp(); }
    @AfterEach void cerrar() throws Exception { super.tearDown(); }
    @Test void rutaPublicaNoConsultaLaSesion() {
        when(auth.contexto(any(),any())).thenThrow(new AccesoException(401,"auth.sesion","Vuelva a iniciar sesión."));
        assertEquals(200,target("publico/tipos-documento").request().get().getStatus());
        assertEquals(200,target("publico/clinicas").request().post(Entity.json("{}")).getStatus());
        assertEquals(200,target("publico/citas").request().post(Entity.json("{}")).getStatus());
        assertEquals(200,target("publico/citas/"+UUID.randomUUID()+"/ticket").request().post(Entity.json("{}")).getStatus());
        verify(auth, never()).contexto(any(), any());
        assertEquals(401,target("tipos-documento").request().get().getStatus());
    }
    @Test void segmentosPuntoNoHeredanLaExclusionPublica() {
        Consulta consulta=new Consulta(UUID.randomUUID()); consulta.setIdPersonaRol(ajeno);
        when(em.find(Consulta.class,consulta.getIdConsulta())).thenReturn(consulta);
        for (String ruta : new String[]{"publico/../consultas/", "publico/%2e%2e/consultas/", "publico/./../consultas/"}) {
            try (Response r = client().target(getBaseUri().toString() + ruta + consulta.getIdConsulta()).request().get()) {
                assertNotEquals(200, r.getStatus(), ruta);
            }
        }
    }
    @Test void pacienteSoloLeeSuExpediente() {
        rol.setCodigo("PACIENTE");
        assertEquals(200,target("personas/"+activo.getIdPersona().getIdPersona()+"/documentos").request().get().getStatus());
        assertEquals(403,target("personas/"+ajeno.getIdPersona().getIdPersona()+"/documentos").request().get().getStatus());
        assertEquals(403,target("consultas").request().get().getStatus());
    }
    @Test void pacienteRegistraDocumentoPropioPeroNoAsignacionesNiConsultas() {
        rol.setCodigo("PACIENTE");
        assertEquals(200,target("personas/"+activo.getIdPersona().getIdPersona()+"/documentos").request().post(Entity.json("{\"valor\":\"123\"}")).getStatus());
        assertEquals(403,target("personas-roles/"+activo.getIdPersonaRol()+"/consultas").request().post(Entity.json("{}")).getStatus());
        assertEquals(403,target("recepcion/pacientes").request().post(Entity.json("{}")).getStatus());
    }
    @Test void doctorRecibeSoloConsultasDeSuClinicaAunqueNoEnvieFiltro() {
        try(Response r=target("consultas").request().get()) { assertEquals(200,r.getStatus()); assertTrue(r.readEntity(String.class).contains("idClinica="+clinica)); }
        assertEquals(403,target("consultas").queryParam("idClinica",ajeno.getIdClinica().getIdClinica()).request().get().getStatus());
    }
    @Test void uuidDirectoYFiltroNoPermitenCruzarClinicas() {
        Consulta consulta=new Consulta(UUID.randomUUID()); consulta.setIdPersonaRol(ajeno);
        when(em.find(Consulta.class,consulta.getIdConsulta())).thenReturn(consulta);
        assertEquals(403,target("consultas/"+consulta.getIdConsulta()).request().get().getStatus());
        assertEquals(403,target("consultas-procedimientos").queryParam("idConsulta",consulta.getIdConsulta()).request().get().getStatus());
    }
    @Test void doctorNoModificaCatalogosNiCreaConsultasFueraDelFlujo() {
        assertEquals(403,target("roles").request().post(Entity.json("{}")).getStatus());
        assertEquals(403,target("consultas").request().post(Entity.json("{}")).getStatus());
        assertEquals(403,target("examenes-resultados").request().post(Entity.json("{}")).getStatus());
    }
    @Test void administracionPreparaConsultaPeroNoEscribeResultados() {
        rol.setCodigo("ADMINISTRACION");
        assertEquals(200,target("personas-roles/"+paciente.getIdPersonaRol()+"/consultas").request().post(Entity.json("{\"observaciones\":\"Control\"}")).getStatus());
        assertEquals(403,target("personas-roles/"+ajeno.getIdPersonaRol()+"/consultas").request().post(Entity.json("{}")).getStatus());
        assertEquals(403,target("examenes-resultados").request().post(Entity.json("{}")).getStatus());
    }
    @Test void referenciasDelCuerpoTambienDebenSerDeLaClinica() {
        assertEquals(403,target("personas-roles/"+paciente.getIdPersonaRol()+"/consultas").request().post(Entity.json("{\"idPersonaRol\":\""+ajeno.getIdPersonaRol()+"\"}")).getStatus());
    }
    @Test void sesionRevocadaSeRechazaAntesDeLeerDatos() {
        when(auth.contexto(any(),any())).thenThrow(new AccesoException(401,"auth.sesion","Sesión finalizada."));
        assertEquals(401,target("consultas").request().get().getStatus());
    }
    @Test void jsonInvalidoYUuidInvalidoDevuelven400() {
        assertEquals(400,target("personas-roles/"+paciente.getIdPersonaRol()+"/consultas").request().post(Entity.json("{")).getStatus());
        assertEquals(400,target("consultas/no-es-uuid").request().get().getStatus());
    }
    @Test void registroDePacientesEstaReservadoAAdministracion() {
        assertEquals(403,target("recepcion/pacientes").request().post(Entity.json("{}")).getStatus());
        rol.setCodigo("ADMINISTRACION");
        assertEquals(200,target("recepcion/pacientes").request().post(Entity.json("{}")).getStatus());
    }
    @Test void doctorUsaCierreYNotasSinBorrarNiReasignarLaConsulta() {
        Consulta consulta=new Consulta(UUID.randomUUID()); consulta.setIdPersonaRol(paciente);
        when(em.find(Consulta.class,consulta.getIdConsulta())).thenReturn(consulta);
        assertEquals(403,target("consultas/"+consulta.getIdConsulta()).request().put(Entity.json("{}" )).getStatus());
    }
    @Test void administracionNoLeeExpedientesNiAsignaPersonalEnOtraClinica() {
        rol.setCodigo("ADMINISTRACION");
        assertEquals(403,target("consultas").request().get().getStatus());
        assertEquals(403,target("clinicas/"+ajeno.getIdClinica().getIdClinica()+"/personas-roles").request().post(Entity.json("{}")).getStatus());
    }
}
