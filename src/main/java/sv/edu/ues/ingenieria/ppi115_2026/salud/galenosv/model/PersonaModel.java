package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import java.util.Collections;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.PersonaService;

@Named("personaModel")
@ViewScoped
public class PersonaModel extends AbstractModel<Persona, UUID> {

    private static final long serialVersionUID = 1L;

    private String nombreBusqueda;
    private int primeraFila;

    @EJB
    private PersonaService personaService;

    @Override
    protected PersonaService getService() {
        return personaService;
    }

    @Override
    protected Persona nuevaInstancia() {
        return new Persona();
    }

    @Override
    protected UUID obtenerId(Persona entidad) {
        return entidad.getIdPersona();
    }

    @Override
    protected void cargarRegistros() {
        primeraFila = 0;
        if (nombreBusqueda == null || nombreBusqueda.isBlank()) {
            super.cargarRegistros();
            return;
        }
        try {
            registros = personaService.buscarPorNombre(nombreBusqueda);
        } catch (ServiceException | EJBException e) {
            registros = Collections.emptyList();
            agregarMensaje(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("error.cargarPersonas"), Mensajes.detalle(e));
        }
    }

    public String getNombreBusqueda() {
        return nombreBusqueda;
    }

    public void setNombreBusqueda(String nombreBusqueda) {
        this.nombreBusqueda = nombreBusqueda;
    }

    public int getPrimeraFila() {
        return primeraFila;
    }

    public void setPrimeraFila(int primeraFila) {
        this.primeraFila = primeraFila;
    }

    public void cargarPersonas() {
        cargarRegistros();
    }

    public java.util.List<Persona> getPersonas() {
        return getRegistros();
    }
}
