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
        var admin=new PersonaRol(UUID.randomUUID()); var rol=new Rol(UUID.randomUUID()); rol.setCodigo("ADMINISTRACION");
        var clinica=new Clinica(UUID.randomUUID()); admin.setIdRol(rol); admin.setIdClinica(clinica);
        when(recurso.atencion.getRolActivo()).thenReturn(admin); when(recurso.autenticacion.contexto(any(),any())).thenReturn(new AutenticacionService.Contexto(new Usuario(),admin));
        var paciente=new Rol(UUID.randomUUID()); paciente.setCodigo("PACIENTE");
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
}
