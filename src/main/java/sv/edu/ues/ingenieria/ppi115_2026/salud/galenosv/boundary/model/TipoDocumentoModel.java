package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

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
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.Mensajes;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

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
        seleccionada = AbstractModel.formularioVacio() ? null : nuevaInstancia();
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

    @FacesConverter(value = "tipoDocumentoConverter", managed = true)
    public static class TipoDocumentoConverter implements Converter<TipoDocumento> {
        @EJB
        private TipoDocumentoService service;

        @Override
        public TipoDocumento getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                TipoDocumento tipo = service.buscarPorId(UUID.fromString(value));
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
        public String getAsString(FacesContext context, UIComponent component, TipoDocumento value) {
            return value == null || value.getIdTipoDocumento() == null ? "" : value.getIdTipoDocumento().toString();
        }
    }
}
