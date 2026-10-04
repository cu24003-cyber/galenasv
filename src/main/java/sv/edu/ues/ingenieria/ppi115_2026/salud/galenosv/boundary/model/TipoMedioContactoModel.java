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
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoMedioContactoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoMedioContacto;

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

    @FacesConverter(value = "tipoMedioContactoConverter", managed = true)
    public static class TipoMedioContactoConverter implements Converter<TipoMedioContacto> {
        @EJB
        private TipoMedioContactoService service;

        @Override
        public TipoMedioContacto getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                TipoMedioContacto tipo = service.buscarPorId(UUID.fromString(value));
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
        public String getAsString(FacesContext context, UIComponent component, TipoMedioContacto value) {
            return value == null || value.getIdTipoMedioContacto() == null ? "" : value.getIdTipoMedioContacto().toString();
        }
    }
}
