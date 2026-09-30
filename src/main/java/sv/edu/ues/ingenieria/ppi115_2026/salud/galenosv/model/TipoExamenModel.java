package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoExamenService;

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
        seleccionada = null;
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
}
