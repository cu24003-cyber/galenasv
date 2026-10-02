package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;

@Named @ViewScoped
public class HistorialConsultaModel implements Serializable {
    @EJB private AtencionService servicio;
    private List<Consulta> consultas = List.of();
    private Consulta seleccionada;
    private List<ConsultaProcedimiento> procedimientos = List.of();
    private List<ConsultaProcedimientoPaso> pasos = List.of();
    private List<OrdenExamen> ordenes = List.of();
    private List<ExamenResultado> resultados = List.of();
    private String ordenId, resultado, interpretacion;

    @PostConstruct public void iniciar() {
        try { consultas = servicio.historial(); }
        catch (ServiceException e) { error(e.getMessage()); }
        catch (EJBException e) { error("No se pudo cargar el historial de consultas."); }
    }
    public void seleccionar(Consulta consulta) {
        try {
            seleccionada = servicio.cargar(consulta.getIdConsulta());
            procedimientos = servicio.realizados(seleccionada.getIdConsulta());
            pasos = servicio.pasos(seleccionada.getIdConsulta());
            ordenes = servicio.ordenes(seleccionada.getIdConsulta());
            resultados = servicio.resultados(seleccionada.getIdConsulta());
        } catch (ServiceException e) { limpiar(); error(e.getMessage()); }
        catch (EJBException e) { limpiar(); error("No se pudo cargar esta consulta."); }
    }
    public void registrarResultado() {
        try {
            if (seleccionada == null || ordenId == null || ordenId.isBlank()) throw new ServiceException("Seleccione una consulta y una orden.");
            servicio.registrarResultado(seleccionada.getIdConsulta(), java.util.UUID.fromString(ordenId), resultado, interpretacion);
            resultado = null; interpretacion = null;
            resultados = servicio.resultados(seleccionada.getIdConsulta());
        } catch (ServiceException | IllegalArgumentException e) { error(e.getMessage()); }
        catch (EJBException e) { error("No se pudo guardar el resultado."); }
    }
    private void limpiar() {
        seleccionada = null; procedimientos = List.of(); pasos = List.of(); ordenes = List.of(); resultados = List.of(); ordenId = null; resultado = null; interpretacion = null;
    }
    private void error(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, mensaje, null));
    }
    public String fecha(OffsetDateTime valor) {
        return valor == null ? "En curso" : valor.atZoneSameInstant(ZoneId.of("America/El_Salvador"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX"));
    }
    public List<Consulta> getConsultas() { return consultas; }
    public Consulta getSeleccionada() { return seleccionada; }
    public List<ConsultaProcedimiento> getProcedimientos() { return procedimientos; }
    public List<ConsultaProcedimientoPaso> getPasos() { return pasos; }
    public List<OrdenExamen> getOrdenes() { return ordenes; }
    public List<ExamenResultado> getResultados() { return resultados; }
    public String fechaResultado(java.util.Date valor) {
        return valor == null ? "Pendiente" : fecha(valor.toInstant().atOffset(java.time.ZoneOffset.UTC));
    }
    public String getOrdenId() { return ordenId; }
    public void setOrdenId(String ordenId) { this.ordenId = ordenId; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getInterpretacion() { return interpretacion; }
    public void setInterpretacion(String interpretacion) { this.interpretacion = interpretacion; }
}
