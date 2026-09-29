package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.util.Collections;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ClinicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

@Named("clinicaModel")
@ViewScoped
public class ClinicaModel extends AbstractModel<Clinica, UUID> {

    private static final long serialVersionUID = 1L;
    private String searchName;
    private int primeraFila;

    @EJB
    private ClinicaService clinicaService;

    @Override
    protected ClinicaService getService() {
        return clinicaService;
    }

    @Override
    protected Clinica nuevaInstancia() {
        return new Clinica();
    }

    @Override
    protected UUID obtenerId(Clinica entidad) {
        return entidad.getIdClinica();
    }

    @Override
    protected void cargarRegistros(){
        primeraFila=0;
        if(searchName==null||searchName.isBlank()){
            super.cargarRegistros();
            return;
        }
        try{
            registros=clinicaService.searchByName(searchName);
        }catch(ServiceException | EJBException e){
            registros = Collections.emptyList();
            agregarMensaje(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("error.cargarClinicas"), Mensajes.detalle(e));
        }
    }
    @Override
    public void editar(Clinica entidad) {
        Clinica copia = new Clinica(entidad.getIdClinica(), entidad.getNombre());
        copia.setActivo(entidad.getActivo());
        copia.setTipo(entidad.getTipo());
        copia.setComentarios(entidad.getComentarios());
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
                clinicaService.crear(seleccionada);
            } else {
                clinicaService.actualizar(seleccionada);
            }
        } catch (ServiceException | EJBException | IllegalArgumentException e) {
            seleccionada.setIdClinica(idOriginal);
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("error.guardar"), Mensajes.detalle(e));
            return;
        }
        seleccionada = null;
        cargarRegistros();
        agregarMensaje(FacesMessage.SEVERITY_INFO, Mensajes.texto("registro.guardado"), null);
    }
    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public int getPrimeraFila() {
        return primeraFila;
    }

    public void setPrimeraFila(int primeraFila) {
        this.primeraFila = primeraFila;
    }

    public void cargarClinicas(){
        cargarRegistros();
    }
    public java.util.List<Clinica> getClinicas(){
        return getRegistros();
    }
}
