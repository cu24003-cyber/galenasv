package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.*;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.application.FacesMessage;
import java.io.Serializable;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;
@Named @ViewScoped
public class CatalogoProcedimientoModel implements Serializable {
    @EJB private ConfiguracionProcedimientoService servicio;
    private Procedimiento seleccionado;
    private List<Procedimiento> lista=List.of();
    private List<ProcedimientoPaso> pasos=List.of();
    private List<ProcedimientoPasoSecuencia> secuencias=List.of();
    private List<Rol> roles=List.of();
    private String nombrePaso, rolId, previoId;
    private boolean fin;
    private List<Examen> examenes=List.of();
    private List<ProcedimientoPasoExamen> asociaciones=List.of();
    private String pasoExamenId, examenId;
    public List<Examen> getExamenes() { return examenes; }
    public List<ProcedimientoPasoExamen> getAsociaciones() { return asociaciones; }
    public String getPasoExamenId() { return pasoExamenId; }
    public void setPasoExamenId(String v) { pasoExamenId=v; }
    public String getExamenId() { return examenId; }
    public void setExamenId(String v) { examenId=v; }
    public void asociar() { ejecutar(()->{servicio.asociar(seleccionado.getIdProcedimiento(),UUID.fromString(pasoExamenId),UUID.fromString(examenId)); cargar();}); }

    @PostConstruct public void iniciar() { nuevo(); ejecutar(()->{ lista=servicio.listar(); roles=servicio.roles(); examenes=servicio.examenes(); }); }
    public void nuevo() { seleccionado=new Procedimiento(); seleccionado.setActivo(true); pasos=List.of(); secuencias=List.of(); }
    public void seleccionar(Procedimiento p) { seleccionado=new Procedimiento(p.getIdProcedimiento()); seleccionado.setNombre(p.getNombre()); seleccionado.setObservaciones(p.getObservaciones()); seleccionado.setActivo(p.getActivo()); ejecutar(this::cargar); }
    private void cargar() { asociaciones=servicio.asociaciones(seleccionado.getIdProcedimiento()); pasos=servicio.pasos(seleccionado.getIdProcedimiento()); secuencias=servicio.secuencias(seleccionado.getIdProcedimiento()); }
    public void guardar() { ejecutar(()->{seleccionado=servicio.guardar(seleccionado); lista=servicio.listar(); cargar();}); }
    public void agregarPaso() { ejecutar(()->{servicio.paso(seleccionado.getIdProcedimiento(),nombrePaso,fin,UUID.fromString(rolId),previoId==null||previoId.isBlank()?null:UUID.fromString(previoId)); nombrePaso=null; previoId=null; fin=false; cargar();}); }
    private void ejecutar(Runnable r) { try { r.run(); } catch(ServiceException e) { error(e.getMessage()); } catch(EJBException | IllegalArgumentException e) { error("No se pudo guardar. Verifique los datos y la conexión."); } }
    private void error(String m) { FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR,m,null)); FacesContext.getCurrentInstance().validationFailed(); }
    public Procedimiento getSeleccionado() { return seleccionado; }
    public List<Procedimiento> getLista() { return lista; }
    public List<ProcedimientoPaso> getPasos() { return pasos; }
    public List<ProcedimientoPasoSecuencia> getSecuencias() { return secuencias; }
    public List<Rol> getRoles() { return roles; }
    public String getNombrePaso() { return nombrePaso; }
    public void setNombrePaso(String v) { nombrePaso=v; }
    public String getRolId() { return rolId; }
    public void setRolId(String v) { rolId=v; }
    public String getPrevioId() { return previoId; }
    public void setPrevioId(String v) { previoId=v; }
    public boolean getFin() { return fin; }
    public void setFin(boolean v) { fin=v; }
}
