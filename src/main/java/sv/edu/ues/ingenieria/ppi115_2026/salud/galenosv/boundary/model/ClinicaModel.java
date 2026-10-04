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
import java.util.Collections;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.Mensajes;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ClinicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Clinica;

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

    @FacesConverter(value = "clinicaConverter", managed = true)
    public static class ClinicaConverter implements Converter<Clinica> {
        @EJB
        private ClinicaService service;

        @Override
        public Clinica getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                Clinica tipo = service.buscarPorId(UUID.fromString(value));
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
        public String getAsString(FacesContext context, UIComponent component, Clinica value) {
            return value == null || value.getIdClinica() == null ? "" : value.getIdClinica().toString();
        }
    }
}
