package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Comparator;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ContextoRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

@Named @ViewScoped
public class CambiarRolModel implements Serializable {
    private static final long serialVersionUID = 1L;
    @EJB private ContextoRolService servicio;
    @Inject private AtencionSesion sesion;
    private List<PersonaRol> asignaciones = List.of();
    private String clinicaId, personaId, asignacionId;

    @PostConstruct public void iniciar() {
        try {
            asignaciones = servicio.listar();
            PersonaRol actual = sesion.getRolActivo();
            if (actual != null) {
                clinicaId = actual.getIdClinica().getIdClinica().toString();
                personaId = actual.getIdPersona().getIdPersona().toString();
                asignacionId = actual.getIdPersonaRol().toString();
            }
        } catch (ServiceException | EJBException e) { error("No se pudieron cargar los roles disponibles."); }
    }

    public String aplicar() {
        try {
            if (clinicaId == null || clinicaId.isBlank() || personaId == null || personaId.isBlank())
                throw new ServiceException("Seleccione una clínica y una persona.");
            cambiarPersona();
            if (asignacionId == null) throw new ServiceException("La persona no tiene un rol activo en la clínica seleccionada.");
            PersonaRol elegida = servicio.cargar(UUID.fromString(asignacionId));
            if (!elegida.getIdClinica().getIdClinica().toString().equals(clinicaId)
                    || !elegida.getIdPersona().getIdPersona().toString().equals(personaId))
                throw new ServiceException("La persona y el rol deben pertenecer a la clínica seleccionada.");
            sesion.cambiarRol(elegida);
            FacesContext contexto = FacesContext.getCurrentInstance();
            if (contexto != null) {
                contexto.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                        MessageFormat.format(Mensajes.texto("clinica.trabajo"), elegida.getIdClinica().getNombre()), null));
                contexto.getExternalContext().getFlash().setKeepMessages(true);
            }
            return sesion.getConsulta() == null ? "/paginas/consultas.xhtml?faces-redirect=true"
                    : "/paginas/consulta.xhtml?faces-redirect=true";
        } catch (ServiceException e) { error(e.getMessage()); }
        catch (EJBException | IllegalArgumentException e) { error("Seleccione una asignación válida de persona, rol y clínica."); }
        return null;
    }

    private void error(String texto) {
        FacesContext contexto = FacesContext.getCurrentInstance();
        contexto.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, texto, null));
        contexto.validationFailed();
    }
    public List<PersonaRol> getAsignaciones() { return asignaciones; }
    public List<Clinica> getClinicas() {
        return asignaciones.stream().map(PersonaRol::getIdClinica).distinct()
                .sorted(Comparator.comparing(Clinica::getNombre)).toList();
    }
    public List<Persona> getPersonas() {
        return asignaciones.stream().filter(r -> r.getIdClinica().getIdClinica().toString().equals(clinicaId))
                .map(PersonaRol::getIdPersona).distinct().toList();
    }
    public List<PersonaRol> getRolesPersona() {
        return asignaciones.stream().filter(r -> r.getIdClinica().getIdClinica().toString().equals(clinicaId)
                && r.getIdPersona().getIdPersona().toString().equals(personaId)).toList();
    }
    public void cambiarClinica() {
        personaId = null; asignacionId = null;
        List<PersonaRol> medicos = asignaciones.stream().filter(r -> r.getIdClinica().getIdClinica().toString().equals(clinicaId)
                && esMedico(r)).toList();
        if (medicos.size() == 1) {
            personaId = medicos.get(0).getIdPersona().getIdPersona().toString();
            asignacionId = medicos.get(0).getIdPersonaRol().toString();
        }
    }
    private static boolean esMedico(PersonaRol asignacion) {
        String nombre = Normalizer.normalize(asignacion.getIdRol().getNombre().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Set.of("medico", "medica", "doctor", "doctora").contains(nombre);
    }
    public void cambiarPersona() {
        List<PersonaRol> roles = getRolesPersona();
        PersonaRol actual = sesion.getRolActivo();
        PersonaRol elegida = roles.stream()
                .filter(r -> actual != null && r.getIdPersonaRol().equals(actual.getIdPersonaRol()))
                .findFirst().orElse(roles.isEmpty() ? null : roles.get(0));
        asignacionId = elegida == null ? null : elegida.getIdPersonaRol().toString();
    }
    public String getClinicaId() { return clinicaId; }
    public void setClinicaId(String id) { clinicaId = id; }
    public String getPersonaId() { return personaId; }
    public void setPersonaId(String id) { personaId = id; }
    public String getAsignacionId() { return asignacionId; }
    public void setAsignacionId(String id) { asignacionId = id; }
}
