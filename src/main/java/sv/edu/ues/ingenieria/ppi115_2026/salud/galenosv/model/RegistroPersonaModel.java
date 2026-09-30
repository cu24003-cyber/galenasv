package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.validation.FormatoExpresion;
import java.util.regex.PatternSyntaxException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;

/** Coordina el registro de una persona y sus relaciones sin enviar otras pestañas. */
@Named("registroPersonaModel")
@ViewScoped
public class RegistroPersonaModel implements Serializable {
    private static final long serialVersionUID = 1L;
    @Inject private PersonaModel personaModel;
    @EJB private PersonaService personaService;
    @EJB private DocumentoService documentoService;
    @EJB private MedioContactoService medioContactoService;
    @EJB private TipoDocumentoService tipoDocumentoService;
    @EJB private TipoMedioContactoService tipoMedioContactoService;

    @EJB private RegistroPersonaService registroService;
    @EJB private RolService rolService;
    @EJB private ClinicaService clinicaService;
    private Rol rol;
    private Clinica clinica;
    private UUID asignacionId;
    private List<PersonaRol> asignaciones = List.of();
    private UUID personaGuardada;
    private Documento documento = new Documento();
    private MedioContacto contacto = new MedioContacto();
    private List<Documento> documentos = List.of();
    private List<MedioContacto> contactos = List.of();
    private int pestana;

    public void nuevo() {
        limpiar();
        personaModel.nuevo();
    }

    public void editar(Persona persona) {
        limpiar();
        personaModel.editar(persona);
        personaGuardada = persona.getIdPersona();
        try {
            asignaciones = registroService.listarPorPersona(personaGuardada);
            if (!asignaciones.isEmpty()) seleccionarAsignacion(asignaciones.get(0));
            documentos = documentoService.listarPorPersona(personaGuardada);
            contactos = medioContactoService.listarPorPersona(personaGuardada);
        } catch (ServiceException | EJBException e) {
            error("error.general");
        }
    }

    public void cancelar() {
        personaModel.cancelar();
        limpiar();
    }

    private void limpiar() {
        personaGuardada = null;
        rol = null;
        clinica = null;
        asignacionId = null;
        asignaciones = List.of();
        documento = new Documento();
        contacto = new MedioContacto();
        documentos = List.of();
        contactos = List.of();
        pestana = 0;
    }

    public void guardarPersona() {
        Persona persona = personaModel.getSeleccionada();
        if (persona == null || persona.getNombres() == null || persona.getNombres().isBlank()
                || persona.getApellidos() == null || persona.getApellidos().isBlank()) {
            error("registro.camposObligatorios");
            return;
        }
        boolean nueva = personaGuardada == null;
        try {
            persona.setNombres(persona.getNombres().trim());
            persona.setApellidos(persona.getApellidos().trim());
            asignacionId = registroService.guardar(persona, rol, clinica, asignacionId);
            personaGuardada = persona.getIdPersona();
        } catch (ServiceException | EJBException e) {
            // El servicio asigna el UUID antes del INSERT; un fallo no habilita las relaciones.
            if (nueva) persona.setIdPersona(null);
            error(e instanceof ServiceException se ? se.getMessageKey() : "error.guardar");
            return;
        }
        try {
            asignaciones = registroService.listarPorPersona(personaGuardada);
        } catch (ServiceException | EJBException e) {
            error("error.general");
        }
        personaModel.cargarPersonas();
        exito();
    }

    public List<TipoDocumento> completarTiposDocumento(String query) {
        try {
            return tipoDocumentoService.listarTodos().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActivo()) && coincide(t.getNombre(), query)).toList();
        } catch (ServiceException | EJBException e) {
            error("error.general");
            return List.of();
        }
    }

    public List<TipoMedioContacto> completarTiposContacto(String query) {
        try {
            return tipoMedioContactoService.listarTodos().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActivo()) && coincide(t.getNombre(), query)).toList();
        } catch (ServiceException | EJBException e) {
            error("error.general");
            return List.of();
        }
    }

    private boolean coincide(String nombre, String query) {
        return nombre != null && nombre.toLowerCase(Locale.ROOT)
                .contains(query == null ? "" : query.trim().toLowerCase(Locale.ROOT));
    }

    public void guardarDocumento() {
        try {
            Persona persona = personaPersistida();
            if (persona == null) return;
            TipoDocumento seleccionado = documento.getIdTipoDocumento();
            TipoDocumento tipo = seleccionado == null ? null
                    : tipoDocumentoService.buscarPorId(seleccionado.getIdTipoDocumento());
            if (tipo == null || !Boolean.TRUE.equals(tipo.getActivo())) {
                error("registro.seleccionInvalida");
                return;
            }
            if (!valorValido(documento.getValor(), tipo.getExpresionRegular())) return;
            documento.setIdPersona(persona);
            documento.setIdTipoDocumento(tipo);
            documento.setValor(documento.getValor().trim());
            documentoService.crear(documento);
        } catch (ServiceException | EJBException e) {
            documento.setIdDocumento(null);
            error("error.guardar");
            return;
        }
        documento = new Documento();
        exito();
        try {
            documentos = documentoService.listarPorPersona(personaGuardada);
        } catch (ServiceException | EJBException e) {
            error("error.general");
        }
    }

    public void guardarContacto() {
        try {
            Persona persona = personaPersistida();
            if (persona == null) return;
            TipoMedioContacto seleccionado = contacto.getIdTipoMedioContacto();
            TipoMedioContacto tipo = seleccionado == null ? null
                    : tipoMedioContactoService.buscarPorId(seleccionado.getIdTipoMedioContacto());
            if (tipo == null || !Boolean.TRUE.equals(tipo.getActivo())) {
                error("registro.seleccionInvalida");
                return;
            }
            if (!valorValido(contacto.getValor(), tipo.getExpresionRegular())) return;
            contacto.setIdPersona(persona);
            contacto.setIdTipoMedioContacto(tipo);
            contacto.setValor(contacto.getValor().trim());
            medioContactoService.crear(contacto);
        } catch (ServiceException | EJBException e) {
            contacto.setIdMedioContacto(null);
            error("error.guardar");
            return;
        }
        contacto = new MedioContacto();
        exito();
        try {
            contactos = medioContactoService.listarPorPersona(personaGuardada);
        } catch (ServiceException | EJBException e) {
            error("error.general");
        }
    }

    private Persona personaPersistida() {
        Persona persona = personaGuardada == null ? null : personaService.buscarPorId(personaGuardada);
        if (persona == null) error("registro.guardarPersonaPrimero");
        return persona;
    }

    private boolean valorValido(String valor, String expresion) {
        if (valor == null || valor.isBlank()) {
            error("registro.camposObligatorios");
            return false;
        }
        // "." es el valor predeterminado del catálogo, sin un formato específico.
        if (expresion == null || expresion.isBlank() || ".".equals(expresion)) return true;
        try {
            if (FormatoExpresion.coincide(valor, expresion)) return true;
            error("registro.formatoInvalido");
        } catch (PatternSyntaxException e) {
            error("registro.patronInvalido");
        }
        return false;
    }

    private void error(String clave) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.validationFailed();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, Mensajes.texto(clave), null));
    }

    private void exito() {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, Mensajes.texto("registro.guardado"), null));
    }

    public String getMascaraDocumento() {
        return FormatoExpresion.mascara(documento.getIdTipoDocumento() == null ? null
                : documento.getIdTipoDocumento().getExpresionRegular());
    }

    public String getMascaraContacto() {
        return FormatoExpresion.mascara(contacto.getIdTipoMedioContacto() == null ? null
                : contacto.getIdTipoMedioContacto().getExpresionRegular());
    }

    public void cancelarDocumento() { documento = new Documento(); }
    public void cancelarContacto() { contacto = new MedioContacto(); }

    public void cambiarTipoDocumento() { documento.setValor(null); }
    public void cambiarTipoContacto() { contacto.setValor(null); }
    public void limpiarTipoDocumento() {
        documento.setIdTipoDocumento(null);
        cambiarTipoDocumento();
    }
    public void limpiarTipoContacto() {
        contacto.setIdTipoMedioContacto(null);
        cambiarTipoContacto();
    }

    public void seleccionarAsignacion(PersonaRol asignacion) {
        asignacionId = asignacion.getIdPersonaRol();
        rol = asignacion.getIdRol();
        clinica = asignacion.getIdClinica();
    }
    public List<PersonaRol> getAsignaciones() { return asignaciones; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Clinica getClinica() { return clinica; }
    public void setClinica(Clinica clinica) { this.clinica = clinica; }
    public java.util.Date getHoy() { return new java.util.Date(); }
    public List<Rol> completarRoles(String query) {
        try {
            return rolService.listarTodos().stream()
                    .filter(r -> Boolean.TRUE.equals(r.getActivo()) && coincide(r.getNombre(), query)).toList();
        } catch (ServiceException | EJBException e) { error("error.general"); return List.of(); }
    }
    public List<Clinica> completarClinicas(String query) {
        try {
            return clinicaService.listarTodos().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getActivo()) && coincide(c.getNombre(), query)).toList();
        } catch (ServiceException | EJBException e) { error("error.general"); return List.of(); }
    }

    public boolean isPersonaGuardada() { return personaGuardada != null; }
    public Documento getDocumento() { return documento; }
    public MedioContacto getContacto() { return contacto; }
    public List<Documento> getDocumentos() { return documentos; }
    public List<MedioContacto> getContactos() { return contactos; }
    public int getPestana() { return pestana; }
    public void setPestana(int pestana) { this.pestana = pestana; }
}
