package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.AbstractService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

/**
 * Clase base para los Managed Beans (Model) de la capa de presentación JSF.
 * Centraliza el ciclo CRUD estándar; delega la lógica de negocio a AbstractService.
 *
 * @param <T>  Tipo de la Entidad
 * @param <ID> Tipo del identificador de la Entidad
 */
public abstract class AbstractModel<T, ID> implements Serializable {

    protected T seleccionada;
    protected List<T> registros;

    protected abstract AbstractService<T, ID> getService();
    protected abstract T nuevaInstancia();
    protected abstract ID obtenerId(T entidad);

    @PostConstruct
    public void init() {
        cargarRegistros();
    }

    protected void cargarRegistros() {
        registros = getService().listarTodos();
    }

    public void nuevo() {
        seleccionada = nuevaInstancia();
    }

    public void editar(T entidad) {
        seleccionada = entidad;
    }

    public void guardar() {
        try {
            if (obtenerId(seleccionada) == null) {
                getService().crear(seleccionada);
            } else {
                getService().actualizar(seleccionada);
            }
            cargarRegistros();
            seleccionada = null;
        } catch (ServiceException e) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Error al guardar", e.getMessage());
        }
    }

    public void eliminar(T entidad) {
        try {
            getService().eliminar(obtenerId(entidad));
            cargarRegistros();
        } catch (ServiceException e) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Error al eliminar", e.getMessage());
        }
    }

    protected void agregarMensaje(FacesMessage.Severity severidad, String resumen, String detalle) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(null, new FacesMessage(severidad, resumen, detalle));
        }
    }

    public T getSeleccionada() {
        return seleccionada;
    }

    public void setSeleccionada(T seleccionada) {
        this.seleccionada = seleccionada;
    }

    public List<T> getRegistros() {
        return registros;
    }
}