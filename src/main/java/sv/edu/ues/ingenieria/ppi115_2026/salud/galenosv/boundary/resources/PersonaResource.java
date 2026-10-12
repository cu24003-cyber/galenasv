package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.PersonaDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ClinicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.RegistroPersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

@Path("personas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class PersonaResource extends AbstractResource<Persona, PersonaDto> {

    @Inject
    private PersonaService service;

    @Inject private RegistroPersonaService registroService;
    @Inject private ClinicaService clinicaService;

    @Override
    protected PersonaDto prepararEnColeccion(PersonaDto datos) {
        return "idClinica".equals(filtroPadre()) ? datos : super.prepararEnColeccion(datos);
    }

    @Override
    protected Persona crearEntidad(UUID id, PersonaDto datos) {
        if (!"idClinica".equals(filtroPadre())) return super.crearEntidad(id, datos);
        Persona persona = new Persona();
        aplicar(datos, persona);
        registroService.guardar(persona, null, relacion(clinicaService, idPadre(), "idClinica"), null);
        return persona;
    }

    @Override
    protected Map<String, String> filtrosPermitidos() {
        return Map.of(
                "idClinica", "personaRolCollection.idClinica.idClinica",
                "idRol", "personaRolCollection.idRol.idRol");
    }

    @Override
    protected Map<String, ColeccionRelacionada> coleccionesRelacionadas() {
        return Map.of(
                "documentos", new ColeccionRelacionada(DocumentoResource.class, "idPersona", CRUD),
                "medios-contacto", new ColeccionRelacionada(MedioContactoResource.class, "idPersona", CRUD),
                "personas-roles", new ColeccionRelacionada(PersonaRolResource.class, "idPersona", CRUD),
                "consultas", new ColeccionRelacionada(ConsultaResource.class, "idPersona"));
    }

    @Override
    protected PersonaService getService() {
        return service;
    }

    @Override
    protected Persona nuevaEntidad(UUID id) {
        return new Persona(id);
    }

    @Override
    protected PersonaDto toDto(Persona entidad) {
        PersonaDto dto = new PersonaDto();
        dto.setIdPersona(entidad.getIdPersona());
        dto.setNombres(entidad.getNombres());
        dto.setApellidos(entidad.getApellidos());
        dto.setFechaNacimiento(entidad.getFechaNacimiento());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        return dto;
    }

    @Override
    protected void aplicar(PersonaDto dto, Persona entidad) {
        entidad.setNombres(dto.getNombres());
        entidad.setApellidos(dto.getApellidos());
        entidad.setFechaNacimiento(dto.getFechaNacimiento());
        entidad.setFechaCreacion(dto.getFechaCreacion());
    }
}
