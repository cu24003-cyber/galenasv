package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.RolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

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
        if (FormularioCancelacion.estaVacio()) rolSeleccionado = null;
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
}
