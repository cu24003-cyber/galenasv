package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.Mensajes;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.RolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

@Named
@ViewScoped
public class RolModel implements Serializable {
    private static final long serialVersionUID = 1L;

    @EJB
    private RolService rolService;
    private List<Rol> roles = new ArrayList<>();
    private Rol rolSeleccionado;
    // Un intento fallido de crear puede asignar UUID antes de fallar en persistencia.
    private boolean nuevo;

    @PostConstruct
    public void init() { cargarRoles(); }

    private void cargarRoles() {
        try {
            roles = rolService.listarTodos();
        } catch (ServiceException | EJBException e) {
            error("error.general", e);
        }
    }

    public void newRol() {
        rolSeleccionado = new Rol();
        rolSeleccionado.setActivo(true);
        nuevo = true;
    }

    public void editRol(Rol rol) {
        // Editar una copia impide que Cancelar altere la fila en memoria.
        rolSeleccionado = new Rol(rol.getIdRol());
        rolSeleccionado.setNombre(rol.getNombre());
        rolSeleccionado.setActivo(rol.getActivo());
        rolSeleccionado.setObservaciones(rol.getObservaciones());
        nuevo = false;
    }

    public void cancelRol() {
        if (AbstractModel.formularioVacio()) rolSeleccionado = null;
        else newRol();
    }

    public void saveRol() {
        if (rolSeleccionado == null || rolSeleccionado.getNombre() == null
                || rolSeleccionado.getNombre().isBlank()) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.validationFailed();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    Mensajes.texto("rol.requerido.nombre"), null));
            return;
        }
        try {
            if (nuevo) {
                rolService.crear(rolSeleccionado);
            } else {
                rolService.actualizar(rolSeleccionado);
            }
        } catch (ServiceException | EJBException e) {
            error("error.guardar", e);
            return;
        }
        rolSeleccionado = null;
        cargarRoles();
    }

    public void deleteRol(Rol rol) {
        if (rol == null || rol.getIdRol() == null) { return; }
        try {
            rolService.eliminar(rol.getIdRol());
            if (rolSeleccionado != null && rol.getIdRol().equals(rolSeleccionado.getIdRol())) {
                rolSeleccionado = null;
            }
            cargarRoles();
        } catch (ServiceException | EJBException e) {
            error("error.eliminar", e);
        }
    }

    private void error(String clave, RuntimeException exception) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.validationFailed();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                Mensajes.texto(clave), Mensajes.detalle(exception)));
    }

    public List<Rol> getRoles() { return roles; }
    public Rol getRolSeleccionado() { return rolSeleccionado; }

    @FacesConverter(value = "rolConverter", managed = true)
    public static class RolConverter implements Converter<Rol> {
        @EJB
        private RolService service;

        @Override
        public Rol getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                Rol tipo = service.buscarPorId(UUID.fromString(value));
                if (tipo != null && Boolean.TRUE.equals(tipo.getActivo())) {
                    return tipo;
                }
            } catch (IllegalArgumentException e) {
                // Solo se aceptan identificadores provenientes del catálogo.
            }
            throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("registro.seleccionInvalida"), null));
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component, Rol value) {
            return value == null || value.getIdRol() == null ? "" : value.getIdRol().toString();
        }
    }
}
