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
import java.util.regex.Pattern;
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
            if (nueva) {
                personaService.crear(persona);
            } else {
                personaService.actualizar(persona);
            }
            personaGuardada = persona.getIdPersona();
        } catch (ServiceException | EJBException e) {
            // El servicio asigna el UUID antes del INSERT; un fallo no habilita las relaciones.
            if (nueva) persona.setIdPersona(null);
            error("error.guardar");
            return;
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
            if (Pattern.matches(expresion, valor.trim())) return true;
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

    public boolean isPersonaGuardada() { return personaGuardada != null; }
    public Documento getDocumento() { return documento; }
    public MedioContacto getContacto() { return contacto; }
    public List<Documento> getDocumentos() { return documentos; }
    public List<MedioContacto> getContactos() { return contactos; }
    public int getPestana() { return pestana; }
    public void setPestana(int pestana) { this.pestana = pestana; }
}
