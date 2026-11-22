package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Date;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

@Path("recepcion/pacientes") @RequestScoped @Transactional
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class RecepcionResource {
    @Inject RegistroPersonaService registro;
    @Inject AutenticacionService autenticacion;
    @Inject AccesoSesion acceso;
    @Inject AtencionSesion atencion;
    @PersistenceContext EntityManager em;
    public static class Paciente {
        @NotBlank @Size(max=255) public String nombres;
        @NotBlank @Size(max=255) public String apellidos;
        @AssertTrue(message = "Los nombres solo admiten letras y espacios.")
        @jakarta.json.bind.annotation.JsonbTransient
        public boolean isNombresValidos() { return nombres == null || nombres.isBlank() || PersonaService.nombreValido(nombres); }
        @AssertTrue(message = "Los apellidos solo admiten letras y espacios.")
        @jakarta.json.bind.annotation.JsonbTransient
        public boolean isApellidosValidos() { return apellidos == null || apellidos.isBlank() || PersonaService.nombreValido(apellidos); }
        @NotNull @PastOrPresent @jakarta.json.bind.annotation.JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX") public Date fechaNacimiento;
    }
    @POST public Response crear(@Valid @NotNull Paciente datos, @Context UriInfo uri) {
        var contexto = autenticacion.contexto(acceso.getIdPersona(), atencion.getRolActivo().getIdPersonaRol());
        if (!"ADMINISTRACION".equals(contexto.asignacion().getIdRol().getCodigo())) throw new AccesoException(403, "auth.permiso", "Se requiere administración.");
        Rol paciente = sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RolRepository.buscarPorCodigo(em, AutenticacionService.PACIENTE);
        if (paciente == null || !Boolean.TRUE.equals(paciente.getActivo())) throw new AccesoException(409, "registro.rolPaciente", "No hay un rol Paciente activo. Créelo o actívelo en Roles antes de registrar pacientes.");
        Persona persona = new Persona(); persona.setNombres(datos.nombres.trim()); persona.setApellidos(datos.apellidos.trim()); persona.setFechaNacimiento(datos.fechaNacimiento);
        var idAsignacion = registro.guardar(persona, paciente, contexto.asignacion().getIdClinica(), null);
        var body = Json.createObjectBuilder().add("idPersona", persona.getIdPersona().toString())
                .add("idPersonaRol", idAsignacion.toString()).build();
        return Response.created(uri.getBaseUriBuilder().path("personas").path(persona.getIdPersona().toString()).build()).entity(body.toString()).build();
    }
}
