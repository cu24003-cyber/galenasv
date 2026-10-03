package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoMedioContactoService;

@Named("tipoMedioContactoModel")
@ViewScoped
public class TipoMedioContactoModel extends AbstractModel<TipoMedioContacto, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private TipoMedioContactoService tipoMedioContactoService;

    @Override
    protected TipoMedioContactoService getService() {
        return tipoMedioContactoService;
    }

    @Override
    protected TipoMedioContacto nuevaInstancia() {
        TipoMedioContacto entidad = new TipoMedioContacto();
        entidad.setActivo(true);
        return entidad;
    }

    @Override
    protected UUID obtenerId(TipoMedioContacto entidad) {
        return entidad.getIdTipoMedioContacto();
    }

    @Override
    public void editar(TipoMedioContacto entidad) {
        seleccionada = null;
        try {
            if (tipoMedioContactoService.estaEnUso(entidad.getIdTipoMedioContacto())) {
                marcarValidacionFallida();
                agregarMensaje(FacesMessage.SEVERITY_WARN, Mensajes.texto("tipoMedioContacto.enUso"), null);
                return;
            }
        } catch (ServiceException | EJBException e) {
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR, Mensajes.texto("error.general"), Mensajes.detalle(e));
            return;
        }
        TipoMedioContacto copia = new TipoMedioContacto(entidad.getIdTipoMedioContacto());
        copia.setNombre(entidad.getNombre());
        copia.setActivo(entidad.getActivo());
        copia.setIndicaciones(entidad.getIndicaciones());
        copia.setExpresionRegular(entidad.getExpresionRegular());
        seleccionada = copia;
    }

    public void cancelar() {
        seleccionada = FormularioCancelacion.estaVacio() ? null : nuevaInstancia();
    }

    @Override
    public void guardar() {
        if (seleccionada == null) {
            marcarValidacionFallida();
            return;
        }
        UUID idOriginal = obtenerId(seleccionada);
        try {
            if (obtenerId(seleccionada) == null) {
                tipoMedioContactoService.crear(seleccionada);
            } else {
                tipoMedioContactoService.actualizar(seleccionada);
            }
        } catch (ServiceException | EJBException | IllegalArgumentException e) {
            seleccionada.setIdTipoMedioContacto(idOriginal);
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("error.guardar"), Mensajes.detalle(e));
            return;
        }
        seleccionada = null;
        cargarRegistros();
        agregarMensaje(FacesMessage.SEVERITY_INFO, Mensajes.texto("registro.guardado"), null);
    }

    @Override
    public void eliminar(TipoMedioContacto entidad) {
        try {
            tipoMedioContactoService.eliminar(obtenerId(entidad));
            if (seleccionada != null && obtenerId(entidad).equals(obtenerId(seleccionada))) {
                seleccionada = null;
            }
            cargarRegistros();
        } catch (ServiceException | EJBException e) {
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("error.eliminar"), Mensajes.detalle(e));
        }
    }
}
