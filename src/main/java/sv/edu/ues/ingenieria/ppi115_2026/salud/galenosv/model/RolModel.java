package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.RolService;

@Named("rolModel")
@ViewScoped
public class RolModel extends AbstractModel<Rol, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private RolService rolService;

    @Override
    protected RolService getService() {
        return rolService;
    }

    @Override
    protected Rol nuevaInstancia() {
        return new Rol();
    }

    @Override
    protected UUID obtenerId(Rol entidad) {
        return entidad.getIdRol();
    }

    public void newRol() {
        nuevo();
    }

    public void editRol(Rol rol) {
        editar(rol);
    }

    public void saveRol() {
        guardar();
    }

    public java.util.List<Rol> getRoles() {
        return getRegistros();
    }

    public Rol getRolSeleccionado() {
        return getSeleccionada();
    }

    public void setRolSeleccionado(Rol rol) {
        setSeleccionada(rol);
    }
}
