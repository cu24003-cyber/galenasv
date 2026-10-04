package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.Mensajes;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Examen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoExamen;

@Named("examenModel")
@ViewScoped
public class ExamenModel extends AbstractModel<Examen, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ExamenService examenService;

    private List<TipoExamen> tipos = List.of();
    private List<String> tiposIds = new ArrayList<>();

    @Override
    @PostConstruct
    public void init() {
        super.init();
        try { tipos = examenService.tipos(); }
        catch (ServiceException | EJBException e) { agregarMensaje(FacesMessage.SEVERITY_ERROR, Mensajes.texto("error.general"), Mensajes.detalle(e)); }
    }

    public List<TipoExamen> getTipos() {
        return tipos.stream().filter(t -> Boolean.TRUE.equals(t.getActivo())
                || (seleccionada != null && seleccionada.getIdExamen() != null && tiposIds != null
                    && tiposIds.contains(t.getIdTipoExamen().toString()))).toList();
    }
    public List<String> getTiposIds() { return tiposIds; }
    public void setTiposIds(List<String> ids) { tiposIds = ids; }

    @Override
    public void nuevo() { super.nuevo(); tiposIds = new ArrayList<>(); }

    @Override
    public void editar(Examen entidad) {
        try {
            List<String> ids = examenService.tiposAsignados(entidad.getIdExamen()).stream().map(UUID::toString).toList();
            Examen copia = new Examen(entidad.getIdExamen());
            copia.setNombre(entidad.getNombre()); copia.setActivo(entidad.getActivo()); copia.setObservaciones(entidad.getObservaciones());
            seleccionada = copia;
            tiposIds = new ArrayList<>(ids);
        } catch (ServiceException | EJBException e) { agregarMensaje(FacesMessage.SEVERITY_ERROR, Mensajes.texto("error.general"), Mensajes.detalle(e)); }
    }

    public void cancelar() {
        if (AbstractModel.formularioVacio()) cerrarEditor();
        else nuevo();
    }

    private void cerrarEditor() { seleccionada = null; tiposIds = new ArrayList<>(); }

    @Override
    public void guardar() {
        if (seleccionada == null) { marcarValidacionFallida(); return; }
        try {
            examenService.guardarConTipos(seleccionada, tiposIds == null ? List.of() : tiposIds.stream().map(UUID::fromString).toList());
            cerrarEditor(); cargarRegistros();
            agregarMensaje(FacesMessage.SEVERITY_INFO, Mensajes.texto("registro.guardado"), null);
        } catch (ServiceException | EJBException | IllegalArgumentException e) {
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR, Mensajes.texto("error.guardar"), e instanceof ServiceException ? e.getMessage() : Mensajes.detalle(e));
        }
    }

    @Override
    public void eliminar(Examen entidad) {
        try {
            examenService.eliminarConTipos(entidad.getIdExamen());
            if (seleccionada != null && entidad.getIdExamen().equals(seleccionada.getIdExamen())) cerrarEditor();
            cargarRegistros();
        } catch (ServiceException | EJBException e) {
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR, Mensajes.texto("error.eliminar"), e instanceof ServiceException ? e.getMessage() : Mensajes.detalle(e));
        }
    }

    @Override
    protected ExamenService getService() {
        return examenService;
    }

    @Override
    protected Examen nuevaInstancia() {
        Examen examen = new Examen();
        examen.setActivo(true);
        return examen;
    }

    @Override
    protected UUID obtenerId(Examen entidad) {
        return entidad.getIdExamen();
    }
}
