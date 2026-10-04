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
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoExamen;

@Named("tipoExamenModel")
@ViewScoped
public class TipoExamenModel extends AbstractModel<TipoExamen, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private TipoExamenService tipoExamenService;

    @Override
    protected TipoExamenService getService() {
        return tipoExamenService;
    }

    @Override
    protected TipoExamen nuevaInstancia() {
        TipoExamen entidad = new TipoExamen();
        entidad.setActivo(true);
        return entidad;
    }

    @Override
    protected UUID obtenerId(TipoExamen entidad) {
        return entidad.getIdTipoExamen();
    }

    @Override
    public void editar(TipoExamen entidad) {
        TipoExamen copia = new TipoExamen(entidad.getIdTipoExamen());
        copia.setNombre(entidad.getNombre());
        copia.setActivo(entidad.getActivo());
        copia.setObservaciones(entidad.getObservaciones());
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
                tipoExamenService.crear(seleccionada);
            } else {
                tipoExamenService.actualizar(seleccionada);
            }
        } catch (ServiceException | EJBException | IllegalArgumentException e) {
            seleccionada.setIdTipoExamen(idOriginal);
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
    public void eliminar(TipoExamen entidad) {
        try {
            tipoExamenService.eliminar(obtenerId(entidad));
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

    @FacesConverter(value = "tipoExamenAtencionConverter", managed = true)
    public static class TipoExamenConverter implements Converter<TipoExamen> {
        @EJB
        private TipoExamenService service;

        @Override
        public TipoExamen getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                TipoExamen tipo = service.buscarPorId(UUID.fromString(value));
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
        public String getAsString(FacesContext context, UIComponent component, TipoExamen value) {
            return value == null || value.getIdTipoExamen() == null ? "" : value.getIdTipoExamen().toString();
        }
    }
}
