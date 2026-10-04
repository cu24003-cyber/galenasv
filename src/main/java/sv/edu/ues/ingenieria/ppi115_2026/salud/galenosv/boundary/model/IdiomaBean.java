package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Locale;
import java.util.Set;

@Named("idiomaBean")
@SessionScoped
public class IdiomaBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Set<String> IDIOMAS = Set.of("es", "en", "fr", "pt", "zh");
    private String idioma = "es";

    public String getIdioma() { return idioma; }

    public void setIdioma(String idioma) {
        if (idioma != null && IDIOMAS.contains(idioma)) {
            this.idioma = idioma;
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null && context.getViewRoot() != null) {
                context.getViewRoot().setLocale(getLocale());
            }
        }
    }

    public Locale getLocale() { return Locale.forLanguageTag(idioma); }
}
