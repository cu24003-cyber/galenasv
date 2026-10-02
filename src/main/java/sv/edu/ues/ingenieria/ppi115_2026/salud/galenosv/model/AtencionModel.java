package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.*;
import java.io.Serializable;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;
@Named @ViewScoped
public class AtencionModel implements Serializable {
    @EJB private AtencionService servicio;
    @Inject private AtencionSesion sesion;
    private Consulta consulta;
    private List<PersonaRol> responsables = List.of();
    private Map<UUID, String> responsableIds = new HashMap<>();
    private List<Procedimiento> procedimientos=List.of();
    private List<ConsultaProcedimiento> realizados=List.of();
    private List<ConsultaProcedimientoPaso> pasos=List.of();
    private List<OrdenExamen> ordenes=List.of();
    private List<ProcedimientoPasoExamen> examenes=List.of();
    private List<ExamenResultado> resultados=List.of();
    private String procedimientoId, notasProcedimiento, pasoId, nombreExamen, notasExamen, indicaciones;
    private String ordenId, resultado, interpretacion;
    private TipoExamen tipo;
    @PostConstruct public void iniciar() {
        if(sesion.getConsulta()!=null && !ejecutar(()->{consulta=servicio.cargar(sesion.getConsulta()); procedimientos=servicio.procedimientos(); recargar();})) {
            consulta = null;
            sesion.setConsulta(null);
        }
    }
    private void recargar() { UUID id=consulta.getIdConsulta(); realizados=servicio.realizados(id); pasos=servicio.pasos(id); ordenes=servicio.ordenes(id); examenes=servicio.examenes(id); resultados=servicio.resultados(id);
        if (consulta.getIdPersonaRol() != null && consulta.getIdPersonaRol().getIdClinica() != null) {
            responsables = servicio.responsables(consulta.getIdPersonaRol().getIdClinica().getIdClinica());
        }
        responsableIds.clear();
        for (ConsultaProcedimientoPaso paso : pasos) responsableIds.put(paso.getIdConsultaProcedimientoPaso(),
                paso.getIdPersonaRol() == null ? null : paso.getIdPersonaRol().getIdPersonaRol().toString());
    }
    private boolean ejecutar(Runnable r) {
        try { r.run(); return true; }
        catch(ServiceException e) { mensaje(Mensajes.mensaje(e)); }
        catch(EJBException | IllegalArgumentException e) { mensaje("No se pudo guardar. Verifique los datos y la conexión."); }
        return false;
    }
    private void mensaje(String m) { FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR,m,null)); FacesContext.getCurrentInstance().validationFailed(); }
    public void guardar() { ejecutar(()->servicio.guardar(consulta.getIdConsulta(),consulta.getReferenciaExterna(),consulta.getObservaciones())); }
    public void agregar() { ejecutar(()->{servicio.agregarProcedimiento(consulta.getIdConsulta(),UUID.fromString(procedimientoId),notasProcedimiento, responsableActivo()); recargar();}); }
    private UUID responsableActivo() {
        if (sesion.getRolActivo() == null) throw new ServiceException("Seleccione el rol y la clínica desde Cambiar de rol.");
        return sesion.getRolActivo().getIdPersonaRol();
    }
    public List<PersonaRol> getResponsables() { return responsables; }
    public Map<UUID, String> getResponsableIds() { return responsableIds; }
    public void asignar(ConsultaProcedimientoPaso paso) {
        ejecutar(() -> {
            String id = responsableIds.get(paso.getIdConsultaProcedimientoPaso());
            if (id == null || id.isBlank()) throw new ServiceException("Seleccione el responsable del paso.");
            servicio.asignarResponsable(consulta.getIdConsulta(), paso.getIdConsultaProcedimientoPaso(), UUID.fromString(id));
            recargar();
        });
    }
    public void completar(ConsultaProcedimientoPaso p) { ejecutar(()->{servicio.completar(consulta.getIdConsulta(),p.getIdConsultaProcedimientoPaso(),p.getValor()); recargar();}); }
    public List<TipoExamen> completarTipos(String q) { return servicio.tipos(q); }
    public void registrarExamen() { ejecutar(()->{servicio.examen(consulta.getIdConsulta(),UUID.fromString(pasoId),nombreExamen,notasExamen,tipo==null?null:tipo.getIdTipoExamen()); nombreExamen=null; notasExamen=null; tipo=null; recargar();}); }
    public void ordenar() { ejecutar(()->{servicio.ordenar(consulta.getIdConsulta(),UUID.fromString(pasoId),indicaciones); indicaciones=null; recargar();}); }
    public void registrarResultado() { ejecutar(()->{servicio.registrarResultado(consulta.getIdConsulta(), UUID.fromString(ordenId), resultado, interpretacion); resultado=null; interpretacion=null; recargar();}); }
    public String cerrar() {
        boolean cerrada = ejecutar(() -> servicio.cerrar(consulta.getIdConsulta(),
                consulta.getReferenciaExterna(), consulta.getObservaciones()));
        if (!cerrada) {
            return null;
        }
        sesion.setConsulta(null);
        return "/paginas/consultas.xhtml?faces-redirect=true";
    }
    public String fecha(java.time.OffsetDateTime f) { return f==null?"Pendiente":f.atZoneSameInstant(java.time.ZoneId.of("America/El_Salvador")).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX")); }
    public String fechaResultado(java.util.Date f) { return f==null?"Pendiente":fecha(f.toInstant().atOffset(java.time.ZoneOffset.UTC)); }
    public Consulta getConsulta() { return consulta; }
    public List<Procedimiento> getProcedimientos() { return procedimientos; }
    public List<ConsultaProcedimiento> getRealizados() { return realizados; }
    public List<ConsultaProcedimientoPaso> getPasos() { return pasos; }
    public List<OrdenExamen> getOrdenes() { return ordenes; }
    public List<ProcedimientoPasoExamen> getExamenes() { return examenes; }
    public List<ExamenResultado> getResultados() { return resultados; }
    public TipoExamen getTipo() { return tipo; }
    public void setTipo(TipoExamen v) { tipo=v; }
    public String getProcedimientoId() { return procedimientoId; }
    public void setProcedimientoId(String v) { procedimientoId=v; }
    public String getNotasProcedimiento() { return notasProcedimiento; }
    public void setNotasProcedimiento(String v) { notasProcedimiento=v; }
    public String getPasoId() { return pasoId; }
    public void setPasoId(String v) { pasoId=v; }
    public String getNombreExamen() { return nombreExamen; }
    public void setNombreExamen(String v) { nombreExamen=v; }
    public String getNotasExamen() { return notasExamen; }
    public void setNotasExamen(String v) { notasExamen=v; }
    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String v) { indicaciones=v; }
    public String getOrdenId() { return ordenId; }
    public void setOrdenId(String v) { ordenId=v; }
    public String getResultado() { return resultado; }
    public void setResultado(String v) { resultado=v; }
    public String getInterpretacion() { return interpretacion; }
    public void setInterpretacion(String v) { interpretacion=v; }
}
