package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.Mensajes;
import jakarta.servlet.http.HttpServletRequest;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AutenticacionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ContextoRolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.IntentosAcceso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

@Named @ViewScoped
public class CambiarRolModel implements Serializable {
    private static final long serialVersionUID = 1L;
    @EJB private ContextoRolService servicio;
    @Inject private AtencionSesion sesion;
    @Inject private AccesoSesion acceso;
    @Inject private IntentosAcceso intentos;
    @EJB private AutenticacionService autenticacion;
    @EJB private TipoDocumentoService tipos;
    private List<TipoDocumento> tiposDocumento;
    private String tipoDocumentoId, numeroDocumento;
    private List<PersonaRol> asignaciones = List.of();
    private List<Clinica> clinicas = List.of();
    private String clinicaId, personaId, asignacionId;

    @PostConstruct public void iniciar() {
        try {
            asignaciones = servicio.listar();
            clinicas = asignaciones.stream().map(PersonaRol::getIdClinica).distinct().toList();
            PersonaRol actual = sesion.getRolActivo();
            if (actual != null) {
                clinicaId = actual.getIdClinica().getIdClinica().toString();
                personaId = actual.getIdPersona().getIdPersona().toString();
                asignacionId = actual.getIdPersonaRol().toString();
            }
        } catch (ServiceException | EJBException e) { error(Mensajes.texto("rolCambio.errorCarga")); }
    }

    public String aplicar() {
        try {
            if (clinicaId == null || clinicaId.isBlank() || personaId == null || personaId.isBlank())
                throw ServiceException.localizada("rolCambio.seleccioneClinicaPersona", "Seleccione una clínica y una persona.");
            cambiarPersona();
            if (asignacionId == null) throw ServiceException.localizada("rolCambio.sinRol", "La persona no tiene un rol activo en la clínica seleccionada.");
            PersonaRol elegida = servicio.cargar(UUID.fromString(asignacionId));
            if (!elegida.getIdClinica().getIdClinica().toString().equals(clinicaId)
                    || !elegida.getIdPersona().getIdPersona().toString().equals(personaId))
                throw ServiceException.localizada("rolCambio.clinicaAjena", "La persona y el rol deben pertenecer a la clínica seleccionada.");
            sesion.cambiarRol(elegida);
            FacesContext contexto = FacesContext.getCurrentInstance();
            if (contexto != null) {
                contexto.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                        MessageFormat.format(Mensajes.texto("clinica.trabajo"), elegida.getIdClinica().getNombre()), null));
                contexto.getExternalContext().getFlash().setKeepMessages(true);
            }
            return sesion.getConsulta() == null ? "/paginas/consultas.xhtml?faces-redirect=true"
                    : "/paginas/consulta.xhtml?faces-redirect=true";
        } catch (ServiceException e) { error(Mensajes.mensaje(e)); }
        catch (EJBException | IllegalArgumentException e) { error(Mensajes.texto("rolCambio.asignacionInvalida")); }
        return null;
    }

    private void error(String texto) {
        FacesContext contexto = FacesContext.getCurrentInstance();
        contexto.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, texto, null));
        contexto.validationFailed();
    }
    public List<PersonaRol> getAsignaciones() { return asignaciones; }
    public List<Clinica> getClinicas() {
        return clinicas.stream().sorted(Comparator.comparing(Clinica::getNombre)).toList();
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
    // --- HU2 en JSF: entrada con documento cuando no hay sesión ---

    public boolean isAutenticado() { return acceso.vigente(); }

    public List<TipoDocumento> getTiposDocumento() {
        if (tiposDocumento == null) tiposDocumento = tipos.listarActivos();
        return tiposDocumento;
    }

    public String entrar() {
        return entrar((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
    }

    /**
     * Mismas reglas y mismo orden que POST /api/auth/login (AuthResource): límite de intentos por
     * dirección, AutenticacionService.iniciar (rechaza Paciente, persona sin rol, rol o clínica
     * inactivos), cambio del id de sesión sin invalidarla y reemplazo del estado anterior.
     */
    String entrar(HttpServletRequest request) {
        UUID tipo;
        try { tipo = tipoDocumentoId == null || tipoDocumentoId.isBlank() ? null : UUID.fromString(tipoDocumentoId); }
        catch (IllegalArgumentException e) { tipo = null; }
        if (tipo == null || numeroDocumento == null || numeroDocumento.isBlank() || numeroDocumento.length() > 50) {
            error(Mensajes.texto("login.datosRequeridos"));
            return null;
        }
        try {
            intentos.registrar(request.getRemoteAddr());
            var contexto = autenticacion.iniciar(tipo, numeroDocumento);
            request.getSession(true);
            request.changeSessionId();
            sesion.limpiar();
            acceso.autenticar(contexto.persona().getIdPersona());
            if (contexto.asignacion() != null) sesion.cambiarRol(contexto.asignacion());
            numeroDocumento = null;
            return "/paginas/cambiar-rol.xhtml?faces-redirect=true";
        } catch (ServiceException e) { error(Mensajes.mensaje(e)); }
        catch (EJBException e) { error(Mensajes.texto("auth.credenciales")); }
        return null;
    }

    public String getTipoDocumentoId() { return tipoDocumentoId; }
    public void setTipoDocumentoId(String id) { tipoDocumentoId = id; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numero) { numeroDocumento = numero; }

    public String getClinicaId() { return clinicaId; }
    public void setClinicaId(String id) { clinicaId = id; }
    public String getPersonaId() { return personaId; }
    public void setPersonaId(String id) { personaId = id; }
    public String getAsignacionId() { return asignacionId; }
    public void setAsignacionId(String id) { asignacionId = id; }
}
