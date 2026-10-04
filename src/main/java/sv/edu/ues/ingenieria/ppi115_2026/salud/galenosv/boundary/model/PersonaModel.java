package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.Mensajes;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

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
    public void editar(Persona entidad) {
        Persona copia = new Persona();
        copia.setIdPersona(entidad.getIdPersona());
        copia.setNombres(entidad.getNombres());
        copia.setApellidos(entidad.getApellidos());
        copia.setFechaNacimiento(entidad.getFechaNacimiento() == null ? null
                : new java.util.Date(entidad.getFechaNacimiento().getTime()));
        copia.setFechaCreacion(entidad.getFechaCreacion() == null ? null
                : new java.util.Date(entidad.getFechaCreacion().getTime()));
        seleccionada = copia;
    }

    public void cancelar() {
        seleccionada = AbstractModel.formularioVacio() ? null : nuevaInstancia();
    }

    public void validarNombre(jakarta.faces.context.FacesContext context,
            jakarta.faces.component.UIComponent campo, Object valor) {
        if (valor == null || valor.toString().isBlank()) return; // required comprueba los vacíos.
        if (!PersonaService.nombreValido(valor.toString())) {
            throw new jakarta.faces.validator.ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("persona.nombreInvalido"), null));
        }
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
