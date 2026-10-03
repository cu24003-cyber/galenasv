package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoDocumento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoDocumentoService;

@Named("tipoDocumentoModel")
@ViewScoped
public class TipoDocumentoModel extends AbstractModel<TipoDocumento, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private TipoDocumentoService tipoDocumentoService;

    @Override
    protected TipoDocumentoService getService() {
        return tipoDocumentoService;
    }

    @Override
    protected TipoDocumento nuevaInstancia() {
        TipoDocumento entidad = new TipoDocumento();
        entidad.setActivo(true);
        return entidad;
    }

    @Override
    protected UUID obtenerId(TipoDocumento entidad) {
        return entidad.getIdTipoDocumento();
    }

    @Override
    public void editar(TipoDocumento entidad) {
        seleccionada = null;
        try {
            if (tipoDocumentoService.estaEnUso(entidad.getIdTipoDocumento())) {
                marcarValidacionFallida();
                agregarMensaje(FacesMessage.SEVERITY_WARN, Mensajes.texto("tipoDocumento.enUso"), null);
                return;
            }
        } catch (ServiceException | EJBException e) {
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR, Mensajes.texto("error.general"), Mensajes.detalle(e));
            return;
        }
        TipoDocumento copia = new TipoDocumento(entidad.getIdTipoDocumento());
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
                tipoDocumentoService.crear(seleccionada);
            } else {
                tipoDocumentoService.actualizar(seleccionada);
            }
        } catch (ServiceException | EJBException | IllegalArgumentException e) {
            seleccionada.setIdTipoDocumento(idOriginal);
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
    public void eliminar(TipoDocumento entidad) {
        try {
            tipoDocumentoService.eliminar(obtenerId(entidad));
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
