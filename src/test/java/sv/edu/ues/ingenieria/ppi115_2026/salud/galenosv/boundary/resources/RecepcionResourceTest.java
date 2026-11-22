package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;
import jakarta.persistence.*;
import jakarta.ws.rs.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

class RecepcionResourceTest {
    @Test @SuppressWarnings("unchecked") void creaPersonaYAsignacionPacienteConLaClinicaDeLaSesion() {
        var recurso=new RecepcionResource(); recurso.registro=mock(RegistroPersonaService.class); recurso.autenticacion=mock(AutenticacionService.class);
        recurso.acceso=mock(AccesoSesion.class); recurso.atencion=mock(AtencionSesion.class); recurso.em=mock(EntityManager.class);
        var admin=new PersonaRol(UUID.randomUUID()); var rol=new Rol(UUID.randomUUID()); rol.setNombre("Atención al público");
        var clinica=new Clinica(UUID.randomUUID()); admin.setIdRol(rol); admin.setIdClinica(clinica);
        when(recurso.atencion.getRolActivo()).thenReturn(admin); when(recurso.autenticacion.contexto(any(),any())).thenReturn(new AutenticacionService.Contexto(new Persona(),admin));
        var paciente=new Rol(UUID.randomUUID()); paciente.setNombre("Paciente"); paciente.setActivo(true);
        TypedQuery<Rol> query=mock(TypedQuery.class); when(recurso.em.createQuery(anyString(),eq(Rol.class))).thenReturn(query); when(query.getResultList()).thenReturn(List.of(paciente));
        UUID personaId=UUID.randomUUID(),asignacionId=UUID.randomUUID();
        when(recurso.registro.guardar(any(),eq(paciente),eq(clinica),isNull())).thenAnswer(i->{ ((Persona)i.getArgument(0)).setIdPersona(personaId);return asignacionId; });
        var datos=new RecepcionResource.Paciente(); datos.nombres=" Ana ";datos.apellidos=" Pérez ";datos.fechaNacimiento=new Date(0);
        var uri=mock(UriInfo.class);when(uri.getBaseUriBuilder()).thenAnswer(i->UriBuilder.fromUri("http://localhost/api/"));
        try(Response response=recurso.crear(datos,uri)) { assertEquals(201,response.getStatus()); assertTrue(response.getEntity().toString().contains(asignacionId.toString())); }
        verify(recurso.registro).guardar(argThat(p->p.getNombres().equals("Ana") && p.getApellidos().equals("Pérez")),eq(paciente),eq(clinica),isNull());
    }
    @Test void rechazaFechaFuturaYCamposVacios() {
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var datos=new RecepcionResource.Paciente(); datos.nombres=" ";datos.apellidos="";datos.fechaNacimiento=new Date(System.currentTimeMillis()+86400000);
            assertEquals(3,factory.getValidator().validate(datos).size());
        }
    }

    @SuppressWarnings("unchecked")
    private static RecepcionResource recursoConRoles(List<Rol> roles) {
        var recurso=new RecepcionResource(); recurso.registro=mock(RegistroPersonaService.class); recurso.autenticacion=mock(AutenticacionService.class);
        recurso.acceso=mock(AccesoSesion.class); recurso.atencion=mock(AtencionSesion.class); recurso.em=mock(EntityManager.class);
        var admin=new PersonaRol(UUID.randomUUID()); var rol=new Rol(UUID.randomUUID()); rol.setNombre("Atencion al publico");
        admin.setIdRol(rol); admin.setIdClinica(new Clinica(UUID.randomUUID()));
        when(recurso.atencion.getRolActivo()).thenReturn(admin); when(recurso.autenticacion.contexto(any(),any())).thenReturn(new AutenticacionService.Contexto(new Persona(),admin));
        TypedQuery<Rol> query=mock(TypedQuery.class); when(recurso.em.createQuery(anyString(),eq(Rol.class))).thenReturn(query); when(query.getResultList()).thenReturn(roles);
        return recurso;
    }
    private static RecepcionResource.Paciente datos() {
        var datos=new RecepcionResource.Paciente(); datos.nombres="Ana";datos.apellidos="Perez";datos.fechaNacimiento=new Date(0); return datos;
    }
    @Test void sinRolPacienteResponde409ConMensajeClaroYNoRegistra() {
        var doctor=new Rol(UUID.randomUUID()); doctor.setNombre("Doctor"); doctor.setActivo(true);
        var recurso=recursoConRoles(List.of(doctor));
        var error=assertThrows(AccesoException.class,()->recurso.crear(datos(),mock(UriInfo.class)));
        assertEquals(409,error.getStatus()); assertEquals("registro.rolPaciente",error.getMessageKey());
        assertTrue(error.getMessage().contains("Paciente"));
        verifyNoInteractions(recurso.registro);
    }
    @Test void conRolPacienteInactivoTambienResponde409() {
        var paciente=new Rol(UUID.randomUUID()); paciente.setNombre("Paciente"); paciente.setActivo(false);
        var recurso=recursoConRoles(List.of(paciente));
        assertEquals(409,assertThrows(AccesoException.class,()->recurso.crear(datos(),mock(UriInfo.class))).getStatus());
        verifyNoInteractions(recurso.registro);
    }
    @Test void reconoceElPacienteEscritoConTildeYMayusculas() {
        var paciente=new Rol(UUID.randomUUID()); paciente.setNombre(" PACIÉNTE "); paciente.setActivo(true);
        var recurso=recursoConRoles(List.of(paciente));
        when(recurso.registro.guardar(any(),eq(paciente),any(),isNull())).thenAnswer(i->{ ((Persona)i.getArgument(0)).setIdPersona(UUID.randomUUID());return UUID.randomUUID(); });
        var uri=mock(UriInfo.class);when(uri.getBaseUriBuilder()).thenAnswer(i->UriBuilder.fromUri("http://localhost/api/"));
        try(Response response=recurso.crear(datos(),uri)) { assertEquals(201,response.getStatus()); }
    }
}
