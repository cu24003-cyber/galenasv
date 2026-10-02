package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;

@Named @ViewScoped
public class HistorialConsultaModel implements Serializable {
    private static final long serialVersionUID = 1L;
    @EJB private AtencionService servicio;
    @Inject private AtencionSesion sesion;
    private List<PersonaRol> pacientes = List.of();
    private String pacienteId, referencia, observaciones, buscarPaciente;
    private LocalDate desde, hasta;
    private UUID contextoId;
    private List<Consulta> consultas = List.of();
    private Consulta seleccionada;
    private List<ConsultaProcedimiento> procedimientos = List.of();
    private List<ConsultaProcedimientoPaso> pasos = List.of();
    private List<OrdenExamen> ordenes = List.of();
    private List<ExamenResultado> resultados = List.of();
    private String ordenId, resultado, interpretacion;

    @PostConstruct public void iniciar() {
        contextoId = sesion.getRolActivo() == null ? null : sesion.getRolActivo().getIdPersonaRol();
        consultas = List.of(); pacientes = List.of(); limpiar();
        if (sesion.getRolActivo() == null) return;
        try { consultas = servicio.historial(); pacientes = servicio.pacientesClinica(); }
        catch (ServiceException e) { error(e.getMessage()); }
        catch (EJBException e) { error("No se pudo cargar el historial de consultas."); }
    }
    public void verificarContexto() {
        UUID actual = sesion.getRolActivo() == null ? null : sesion.getRolActivo().getIdPersonaRol();
        if (!Objects.equals(contextoId, actual)) {
            pacienteId = null; referencia = null; observaciones = null; buscarPaciente = null; desde = null; hasta = null;
            iniciar();
        }
    }
    public void seleccionar(Consulta consulta) {
        limpiar();
        try {
            seleccionada = servicio.cargar(consulta.getIdConsulta());
            procedimientos = servicio.realizados(seleccionada.getIdConsulta());
            pasos = servicio.pasos(seleccionada.getIdConsulta());
            ordenes = servicio.ordenes(seleccionada.getIdConsulta());
            resultados = servicio.resultados(seleccionada.getIdConsulta());
        } catch (ServiceException e) { limpiar(); error(e.getMessage()); }
        catch (EJBException e) { limpiar(); error("No se pudo cargar esta consulta."); }
    }
    public String crear() {
        try {
            if (pacienteId == null || pacienteId.isBlank()) throw new ServiceException("Seleccione un paciente de su clínica.");
            Consulta nueva = servicio.crear(java.util.UUID.fromString(pacienteId), referencia, observaciones);
            sesion.setConsulta(nueva.getIdConsulta());
            return "/paginas/consulta.xhtml?faces-redirect=true";
        } catch (ServiceException e) { error(e.getMessage()); }
        catch (EJBException | IllegalArgumentException e) { error("No se pudo crear la consulta. Verifique el paciente y los datos."); }
        return null;
    }
    public String retomar(Consulta consulta) {
        try {
            Consulta propia = servicio.cargar(consulta.getIdConsulta());
            if (propia.getFechaFin() != null) throw new ServiceException("La consulta ya está cerrada.");
            if (sesion.getConsulta() != null && !sesion.getConsulta().equals(propia.getIdConsulta()))
                throw new ServiceException("Cierre la consulta en curso antes de retomar otra.");
            sesion.setConsulta(propia.getIdConsulta());
            return "/paginas/consulta.xhtml?faces-redirect=true";
        } catch (ServiceException e) { error(e.getMessage()); }
        catch (EJBException e) { error("No se pudo retomar esta consulta."); }
        return null;
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
        FacesContext.getCurrentInstance().validationFailed();
    }
    public String fecha(OffsetDateTime valor) {
        return valor == null ? "En curso" : valor.atZoneSameInstant(ZoneId.of("America/El_Salvador"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX"));
    }
    public List<Consulta> getConsultas() { return consultas; }
    public List<PersonaRol> getPacientes() { return pacientes; }
    public List<PersonaRol> getPacientesFiltrados() {
        if (buscarPaciente == null || buscarPaciente.isBlank()) return pacientes;
        String patron = buscarPaciente.trim().toLowerCase(java.util.Locale.ROOT);
        return pacientes.stream().filter(r -> r.getIdPersonaRol().toString().equals(pacienteId)
                || (r.getIdPersona().getNombres() + " " + r.getIdPersona().getApellidos())
                    .toLowerCase(java.util.Locale.ROOT).contains(patron)).toList();
    }
    public String getBuscarPaciente() { return buscarPaciente; }
    public void setBuscarPaciente(String buscarPaciente) { this.buscarPaciente = buscarPaciente; }
    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
    public void filtrar() {
        try { consultas = servicio.historial(desde, hasta); }
        catch (ServiceException e) { error(Mensajes.mensaje(e)); }
        catch (EJBException e) { error(Mensajes.texto("consulta.errorFiltro")); }
    }
    public void limpiarFiltro() { desde = null; hasta = null; filtrar(); }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String id) { pacienteId = id; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String valor) { referencia = valor; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String valor) { observaciones = valor; }
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
