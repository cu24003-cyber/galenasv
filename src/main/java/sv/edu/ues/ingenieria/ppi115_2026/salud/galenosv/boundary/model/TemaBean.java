package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import org.primefaces.PrimeFaces;

@Named("temaBean")
@SessionScoped
public class TemaBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean oscuro = true;

    public boolean isOscuro() {
        return oscuro;
    }

    public void setOscuro(boolean oscuro) {
        this.oscuro = oscuro;
    }

    public String getTema() {
        return oscuro ? "vela-blue" : "saga-blue";
    }

    public void aplicarTema() {
        PrimeFaces.current().executeScript("PrimeFaces.changeTheme('" + getTema()
                + "');document.body.style.colorScheme='" + (oscuro ? "dark" : "light") + "';");
    }
}
