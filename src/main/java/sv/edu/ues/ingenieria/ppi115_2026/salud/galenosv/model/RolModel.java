package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;

import jakarta.inject.Named;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.RolService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class RolModel implements Serializable{
    @EJB
    private RolService rolService;
    private List<Rol> roles;
    private Rol rolSeleccionado;
    @jakarta.annotation.PostConstruct
    public void init(){
        cargarRoles();
    }
    private void cargarRoles(){
        roles = rolService.listarTodos();
    }
    public void newRol(){
        rolSeleccionado=new Rol();
    }
    public void editRol(Rol r){
        rolSeleccionado=r;
    }
    public void saveRol(){
        try{
            if(rolSeleccionado.getIdRol()==null){
                rolService.crear(rolSeleccionado);
            }else{
                rolService.actualizar(rolSeleccionado);
            }
            cargarRoles();
            rolSeleccionado=null;
        }catch(ServiceException e){
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al guardar", e.getMessage()));
        }
    }
}
